package com.newsrss.service.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.newsrss.common.ResourceNotFoundException;
import com.newsrss.config.DeepSeekProperties;
import com.newsrss.domain.entity.RssFeed;
import com.newsrss.dto.parser.ParserTemplateGenerateRequest;
import com.newsrss.dto.parser.ParserTemplateGenerateResponse;
import com.newsrss.dto.parser.ParserTemplatePreviewRequest;
import com.newsrss.dto.parser.ParserTemplatePreviewResponse;
import com.newsrss.dto.parser.ParserTemplateRequest;
import com.newsrss.repository.RssFeedRepository;
import com.newsrss.service.rss.RssFeedParser;
import com.newsrss.service.rss.RssFeedParser.RawFeedSample;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * 解析模板生成服务，负责基于真实订阅源样本生成标准字段和自定义字段映射。
 */
@Service
@Profile("db")
public class ParserTemplateGenerationService {

    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 10;
    private static final Set<String> STANDARD_FIELDS = Set.of(
            "guid",
            "articleUrl",
            "title",
            "summary",
            "author",
            "publishedAt",
            "contentHtml",
            "coverImageUrl");

    private final RssFeedRepository feedRepository;
    private final RssFeedParser feedParser;
    private final ParserTemplatePreviewService previewService;
    private final ObjectMapper objectMapper;
    private final DeepSeekProperties deepSeekProperties;

    public ParserTemplateGenerationService(
            RssFeedRepository feedRepository,
            RssFeedParser feedParser,
            ParserTemplatePreviewService previewService,
            ObjectMapper objectMapper,
            DeepSeekProperties deepSeekProperties) {
        this.feedRepository = feedRepository;
        this.feedParser = feedParser;
        this.previewService = previewService;
        this.objectMapper = objectMapper;
        this.deepSeekProperties = deepSeekProperties;
    }

    /**
     * 从订阅源真实样本生成解析模板，并自动预览生成效果。
     *
     * @param request 生成请求
     * @return 生成结果
     */
    public ParserTemplateGenerateResponse generateFromFeed(ParserTemplateGenerateRequest request) {
        RssFeed feed = request.feedId() == null ? null : findFeed(request.feedId());
        String feedUrl = resolveFeedUrl(request, feed);
        int limit = normalizeLimit(request.limit());
        RawFeedSample sample = feedParser.sampleRawPayloads(feedUrl, limit);
        List<String> warnings = new ArrayList<>();
        ParserTemplateRequest fallbackTemplate = buildLocalTemplate(feedUrl, feed, sample, warnings);
        ParserTemplateRequest generatedTemplate = fallbackTemplate;
        boolean aiUsed = false;
        boolean fallbackUsed = true;

        if (Boolean.TRUE.equals(request.preferAi()) && deepSeekProperties.callable()) {
            try {
                generatedTemplate = validateAiTemplate(callDeepSeek(feedUrl, sample, fallbackTemplate), fallbackTemplate);
                aiUsed = true;
                fallbackUsed = false;
            } catch (RuntimeException exception) {
                warnings.add("DeepSeek 生成失败，已使用本地生成模板：" + exception.getMessage());
            }
        } else if (Boolean.TRUE.equals(request.preferAi())) {
            warnings.add("未配置 DeepSeek API Key，已使用本地生成模板");
        }

        ParserTemplatePreviewResponse preview = previewService.preview(new ParserTemplatePreviewRequest(
                feedUrl,
                null,
                toPreviewPayload(generatedTemplate),
                limit));
        return new ParserTemplateGenerateResponse(
                feed == null ? null : feed.getId(),
                feedUrl,
                sample.title(),
                fallbackUsed ? "local-heuristic" : deepSeekProperties.resolvedModel(),
                aiUsed,
                fallbackUsed,
                generatedTemplate,
                preview,
                sample.payloads(),
                List.copyOf(warnings));
    }

    /**
     * 查询订阅源实体。
     *
     * @param feedId 订阅源主键
     * @return 订阅源实体
     */
    private RssFeed findFeed(Long feedId) {
        return feedRepository.findById(feedId)
                .orElseThrow(() -> new ResourceNotFoundException("订阅源不存在：" + feedId));
    }

    /**
     * 解析本次生成使用的订阅源地址。
     *
     * @param request 生成请求
     * @param feed 已保存订阅源
     * @return RSS 或 Atom 地址
     */
    private String resolveFeedUrl(ParserTemplateGenerateRequest request, RssFeed feed) {
        if (feed != null) {
            return feed.getFeedUrl();
        }
        String feedUrl = request.feedUrl();
        if (feedUrl == null || feedUrl.isBlank()) {
            throw new IllegalArgumentException("请选择订阅源或输入 RSS 地址");
        }
        return feedUrl.strip();
    }

    /**
     * 构建本地启发式模板。
     *
     * @param feedUrl RSS 或 Atom 地址
     * @param feed 已保存订阅源
     * @param sample 原始样本
     * @param warnings 生成提示
     * @return 模板请求
     */
    private ParserTemplateRequest buildLocalTemplate(
            String feedUrl,
            RssFeed feed,
            RawFeedSample sample,
            List<String> warnings) {
        List<PathSample> pathSamples = flattenPayloads(sample.payloads());
        Map<String, List<String>> fieldMapping = new LinkedHashMap<>();
        fieldMapping.put("guid", choosePaths(pathSamples, "guid", 3));
        fieldMapping.put("articleUrl", choosePaths(pathSamples, "articleUrl", 4));
        fieldMapping.put("title", choosePaths(pathSamples, "title", 4));
        fieldMapping.put("summary", choosePaths(pathSamples, "summary", 4));
        fieldMapping.put("author", choosePaths(pathSamples, "author", 3));
        fieldMapping.put("publishedAt", choosePaths(pathSamples, "publishedAt", 4));
        fieldMapping.put("contentHtml", choosePaths(pathSamples, "contentHtml", 4));
        fieldMapping.put("coverImageUrl", choosePaths(pathSamples, "coverImageUrl", 4));
        ensureRequiredFallbacks(fieldMapping, warnings);

        List<String> contentSelectors = fieldMapping.getOrDefault("contentHtml", List.of());
        List<String> coverSelectors = fieldMapping.getOrDefault("coverImageUrl", List.of());
        Map<String, List<String>> customFieldMapping = chooseCustomFields(pathSamples, fieldMapping);
        String feedName = feed == null ? sample.title() : feed.getFeedName();
        String templateCode = slugify(feedName == null ? feedUrl : feedName);
        String templateName = (feedName == null || feedName.isBlank() ? "AI 生成模板" : feedName.strip()) + " 解析模板";
        ObjectNode cleanupRules = objectMapper.createObjectNode();
        cleanupRules.set("removeSelectors", objectMapper.valueToTree(List.of("script", "style", ".ad", ".advertisement")));
        cleanupRules.set("unwrapSelectors", objectMapper.valueToTree(List.of("article")));
        cleanupRules.set("removeAttributes", objectMapper.valueToTree(List.of("onclick", "onload", "style")));
        return new ParserTemplateRequest(
                templateCode,
                templateName,
                "基于真实 RSS 样本生成，标准字段优先入库，多余字段进入 customFields。",
                objectMapper.valueToTree(fieldMapping),
                objectMapper.valueToTree(customFieldMapping),
                objectMapper.valueToTree(contentSelectors),
                objectMapper.valueToTree(coverSelectors),
                objectMapper.valueToTree(List.of("yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd")),
                cleanupRules,
                true);
    }

    /**
     * 使用 DeepSeek 生成模板配置。
     *
     * @param feedUrl RSS 或 Atom 地址
     * @param sample 原始样本
     * @param fallbackTemplate 本地兜底模板
     * @return DeepSeek 生成模板
     */
    private ParserTemplateRequest callDeepSeek(String feedUrl, RawFeedSample sample, ParserTemplateRequest fallbackTemplate) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(deepSeekProperties.resolvedTimeoutSeconds()));
        requestFactory.setReadTimeout(Duration.ofSeconds(deepSeekProperties.resolvedTimeoutSeconds()));
        RestClient restClient = RestClient.builder()
                .baseUrl(deepSeekProperties.resolvedBaseUrl())
                .requestFactory(requestFactory)
                .build();
        DeepSeekRequest request = new DeepSeekRequest(
                deepSeekProperties.resolvedModel(),
                List.of(
                        new DeepSeekMessage("system", deepSeekSystemPrompt()),
                        new DeepSeekMessage("user", deepSeekUserPrompt(feedUrl, sample, fallbackTemplate))),
                new ResponseFormat("json_object"),
                0.1);
        DeepSeekResponse response = restClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + deepSeekProperties.apiKey().strip())
                .body(request)
                .retrieve()
                .body(DeepSeekResponse.class);
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new IllegalStateException("DeepSeek 返回为空");
        }
        String content = response.choices().get(0).message().content();
        try {
            return objectMapper.readValue(content, ParserTemplateRequest.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("DeepSeek 返回不是合法模板 JSON", exception);
        }
    }

    /**
     * 生成 DeepSeek 系统提示词。
     *
     * @return 系统提示词
     */
    private String deepSeekSystemPrompt() {
        return """
                你是 RSS 解析模板生成器。只返回 JSON，不要返回 Markdown。
                JSON 必须匹配字段：templateCode, templateName, description, fieldMapping, customFieldMapping,
                contentSelectors, coverSelectors, timeFormats, cleanupRules, enabled。
                fieldMapping 只放标准字段：guid, articleUrl, title, summary, author, publishedAt, contentHtml, coverImageUrl。
                标准字段必须优先从真实 payload 路径中选择，articleUrl 和 title 必须尽量命中。
                标准字段之外的有价值字段必须放入 customFieldMapping，不要丢弃。
                路径只允许使用点号和数字数组下标，例如 contents[0].value，不要使用通配符。
                """;
    }

    /**
     * 生成 DeepSeek 用户提示词。
     *
     * @param feedUrl RSS 或 Atom 地址
     * @param sample 原始样本
     * @param fallbackTemplate 本地兜底模板
     * @return 用户提示词
     */
    private String deepSeekUserPrompt(String feedUrl, RawFeedSample sample, ParserTemplateRequest fallbackTemplate) {
        try {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("feedUrl", feedUrl);
            payload.put("feedTitle", sample.title());
            payload.set("samplePayloads", objectMapper.valueToTree(sample.payloads()));
            payload.set("fallbackTemplate", objectMapper.valueToTree(fallbackTemplate));
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("构造 DeepSeek 提示失败", exception);
        }
    }

    /**
     * 校验 AI 生成模板，缺失关键字段时回退本地模板。
     *
     * @param aiTemplate AI 模板
     * @param fallbackTemplate 本地模板
     * @return 可用模板
     */
    private ParserTemplateRequest validateAiTemplate(ParserTemplateRequest aiTemplate, ParserTemplateRequest fallbackTemplate) {
        if (aiTemplate == null
                || isBlank(aiTemplate.templateCode())
                || isBlank(aiTemplate.templateName())
                || !hasMapping(aiTemplate.fieldMapping(), "title")
                || !hasMapping(aiTemplate.fieldMapping(), "articleUrl")) {
            return fallbackTemplate;
        }
        return aiTemplate;
    }

    /**
     * 判断映射是否包含指定字段。
     *
     * @param mapping 映射 JSON
     * @param fieldName 字段名
     * @return 包含时返回 true
     */
    private boolean hasMapping(JsonNode mapping, String fieldName) {
        JsonNode value = mapping == null ? null : mapping.get(fieldName);
        return value != null && value.isArray() && value.size() > 0;
    }

    /**
     * 将保存模板请求转换成预览模板载荷。
     *
     * @param request 保存模板请求
     * @return 预览模板载荷
     */
    private ParserTemplatePreviewRequest.PreviewTemplatePayload toPreviewPayload(ParserTemplateRequest request) {
        return new ParserTemplatePreviewRequest.PreviewTemplatePayload(
                request.templateCode(),
                request.templateName(),
                request.description(),
                request.fieldMapping(),
                request.customFieldMapping(),
                request.contentSelectors(),
                request.coverSelectors(),
                request.timeFormats(),
                request.cleanupRules(),
                request.enabled());
    }

    /**
     * 展平样本字段，供本地生成器打分。
     *
     * @param payloads 原始样本
     * @return 字段样本
     */
    private List<PathSample> flattenPayloads(List<JsonNode> payloads) {
        Map<String, PathSampleBuilder> builders = new LinkedHashMap<>();
        for (JsonNode payload : payloads) {
            flattenNode(payload, "", builders);
        }
        return builders.values().stream()
                .map(PathSampleBuilder::build)
                .toList();
    }

    /**
     * 递归展平 JSON 节点。
     *
     * @param node 当前节点
     * @param path 当前路径
     * @param builders 字段聚合器
     */
    private void flattenNode(JsonNode node, String path, Map<String, PathSampleBuilder> builders) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isValueNode()) {
            String value = node.asText();
            if (!isBlank(value) && !path.isBlank()) {
                builders.computeIfAbsent(path, PathSampleBuilder::new).add(value);
            }
            return;
        }
        if (node.isArray()) {
            for (int index = 0; index < Math.min(node.size(), 2); index++) {
                flattenNode(node.get(index), path + "[" + index + "]", builders);
            }
            return;
        }
        node.fields().forEachRemaining(entry -> {
            String nextPath = path.isBlank() ? entry.getKey() : path + "." + entry.getKey();
            flattenNode(entry.getValue(), nextPath, builders);
        });
    }

    /**
     * 按字段语义选择候选路径。
     *
     * @param samples 字段样本
     * @param fieldName 标准字段名
     * @param limit 最大路径数量
     * @return 候选路径
     */
    private List<String> choosePaths(List<PathSample> samples, String fieldName, int limit) {
        return samples.stream()
                .filter(sample -> !isNoisyPath(sample.path()))
                .map(sample -> new ScoredPath(sample.path(), scorePath(sample, fieldName)))
                .filter(scoredPath -> scoredPath.score() >= minimumScore(fieldName))
                .sorted(Comparator.comparingInt(ScoredPath::score).reversed().thenComparing(ScoredPath::path))
                .limit(limit)
                .map(ScoredPath::path)
                .toList();
    }

    /**
     * 获取不同标准字段的最低候选分，避免低置信路径污染模板。
     *
     * @param fieldName 标准字段名
     * @return 最低分
     */
    private int minimumScore(String fieldName) {
        return switch (fieldName) {
            case "title", "summary", "contentHtml" -> 70;
            case "articleUrl", "publishedAt", "coverImageUrl" -> 60;
            default -> 50;
        };
    }

    /**
     * 为路径和目标字段计算匹配分。
     *
     * @param sample 字段样本
     * @param fieldName 标准字段名
     * @return 分数
     */
    private int scorePath(PathSample sample, String fieldName) {
        String path = sample.path().toLowerCase(Locale.ROOT);
        String value = sample.firstValue().toLowerCase(Locale.ROOT);
        int score = 0;
        if ("guid".equals(fieldName)) {
            score += path.equals("uri") ? 100 : 0;
            score += path.contains("guid") || path.endsWith(".id") ? 80 : 0;
        } else if ("articleUrl".equals(fieldName)) {
            score += path.equals("link") ? 100 : 0;
            score += path.contains("url") || path.contains("href") || path.contains("link") ? 70 : 0;
            score += looksLikeUrl(value) ? 30 : 0;
        } else if ("title".equals(fieldName)) {
            score += path.equals("title") ? 100 : 0;
            score += path.contains("title") || path.contains("headline") ? 80 : 0;
            score += sample.averageLength() >= 6 && sample.averageLength() <= 180 ? 10 : 0;
        } else if ("summary".equals(fieldName)) {
            score += path.contains("description") || path.contains("summary") || path.contains("abstract") ? 90 : 0;
            score += sample.averageLength() >= 20 ? 20 : 0;
        } else if ("author".equals(fieldName)) {
            score += path.equals("author") || path.endsWith(".author") ? 100 : 0;
            score += path.contains("authors") && path.endsWith(".name") ? 80 : 0;
        } else if ("publishedAt".equals(fieldName)) {
            score += path.contains("publisheddate") || path.contains("updateddate") ? 100 : 0;
            score += path.contains("date") || path.contains("time") || path.contains("published") ? 70 : 0;
        } else if ("contentHtml".equals(fieldName)) {
            score += path.contains("contents") && path.endsWith(".value") ? 100 : 0;
            score += path.contains("content") || path.contains("body") || path.contains("description.value") ? 70 : 0;
            score += value.contains("<") || sample.averageLength() > 120 ? 30 : 0;
        } else if ("coverImageUrl".equals(fieldName)) {
            score += path.contains("enclosures") && path.endsWith(".url") ? 100 : 0;
            score += path.contains("image") || path.contains("cover") || path.contains("thumbnail") ? 80 : 0;
            score += looksLikeImageUrl(value) ? 30 : 0;
        }
        return score;
    }

    /**
     * 选择标准字段之外的自定义字段映射。
     *
     * @param samples 字段样本
     * @param standardMapping 标准字段映射
     * @return 自定义字段映射
     */
    private Map<String, List<String>> chooseCustomFields(List<PathSample> samples, Map<String, List<String>> standardMapping) {
        Set<String> usedPaths = new LinkedHashSet<>();
        standardMapping.values().forEach(usedPaths::addAll);
        Map<String, List<String>> customMapping = new LinkedHashMap<>();
        samples.stream()
                .filter(sample -> !usedPaths.contains(sample.path()))
                .filter(sample -> !isNoisyPath(sample.path()))
                .filter(sample -> isUsefulCustomPath(sample.path(), sample.firstValue()))
                .limit(12)
                .forEach(sample -> customMapping.put(customFieldName(sample.path()), List.of(sample.path())));
        return customMapping;
    }

    /**
     * 补充关键标准字段兜底路径。
     *
     * @param fieldMapping 标准字段映射
     * @param warnings 生成提示
     */
    private void ensureRequiredFallbacks(Map<String, List<String>> fieldMapping, List<String> warnings) {
        ensureFallback(fieldMapping, "articleUrl", List.of("link", "links[0].href"), warnings);
        ensureFallback(fieldMapping, "title", List.of("title"), warnings);
        ensureFallback(fieldMapping, "summary", List.of("description.value"), warnings);
        ensureFallback(fieldMapping, "contentHtml", List.of("contents[0].value", "description.value"), warnings);
    }

    /**
     * 如果字段没有候选路径，则填入默认路径。
     *
     * @param fieldMapping 标准字段映射
     * @param fieldName 字段名
     * @param fallbackPaths 兜底路径
     * @param warnings 生成提示
     */
    private void ensureFallback(
            Map<String, List<String>> fieldMapping,
            String fieldName,
            List<String> fallbackPaths,
            List<String> warnings) {
        if (fieldMapping.getOrDefault(fieldName, List.of()).isEmpty()) {
            fieldMapping.put(fieldName, fallbackPaths);
            warnings.add(fieldName + " 未识别到高置信路径，已加入默认兜底路径");
        }
    }

    /**
     * 判断路径是否不适合进入映射。
     *
     * @param path 字段路径
     * @return 噪音字段返回 true
     */
    private boolean isNoisyPath(String path) {
        String normalizedPath = path.toLowerCase(Locale.ROOT);
        return normalizedPath.endsWith(".mode")
                || normalizedPath.endsWith(".type")
                || normalizedPath.endsWith(".length")
                || normalizedPath.endsWith(".rel")
                || normalizedPath.endsWith(".hreflang")
                || normalizedPath.endsWith(".attributes");
    }

    /**
     * 判断路径是否适合作为自定义字段。
     *
     * @param path 字段路径
     * @param value 样本文本
     * @return 适合时返回 true
     */
    private boolean isUsefulCustomPath(String path, String value) {
        String normalizedPath = path.toLowerCase(Locale.ROOT);
        if (isBlank(value) || value.length() > 500) {
            return false;
        }
        return normalizedPath.contains("comment")
                || normalizedPath.contains("category")
                || normalizedPath.contains("source")
                || normalizedPath.contains("tag")
                || normalizedPath.contains("media")
                || normalizedPath.contains("foreignmarkup")
                || normalizedPath.contains("report")
                || normalizedPath.contains("industry");
    }

    /**
     * 将路径转换为自定义字段名。
     *
     * @param path 字段路径
     * @return 自定义字段名
     */
    private String customFieldName(String path) {
        String[] parts = path.replaceAll("\\[\\d+]", "").split("\\.");
        String lastPart = parts[parts.length - 1];
        if ("value".equals(lastPart) || "text".equals(lastPart)) {
            lastPart = parts.length >= 2 ? parts[parts.length - 2] : lastPart;
        }
        String normalized = lastPart.replaceAll("[^A-Za-z0-9]+", " ");
        String[] words = normalized.trim().split("\\s+");
        if (words.length == 0 || words[0].isBlank()) {
            return "customField";
        }
        StringBuilder builder = new StringBuilder(words[0].toLowerCase(Locale.ROOT));
        for (int index = 1; index < words.length; index++) {
            builder.append(Character.toUpperCase(words[index].charAt(0)))
                    .append(words[index].substring(1).toLowerCase(Locale.ROOT));
        }
        String fieldName = builder.toString();
        return STANDARD_FIELDS.contains(fieldName) ? fieldName + "Extra" : fieldName;
    }

    /**
     * 将名称转换成模板编码。
     *
     * @param value 原始名称
     * @return 模板编码
     */
    private String slugify(String value) {
        String normalized = value == null ? "generated-template" : value.toLowerCase(Locale.ROOT)
                .replaceAll("https?://", "")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (normalized.isBlank()) {
            return "generated-template";
        }
        return (normalized.length() > 60 ? normalized.substring(0, 60) : normalized) + "-ai";
    }

    /**
     * 归一化样本条数。
     *
     * @param limit 请求条数
     * @return 实际条数
     */
    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("样本条数必须大于 0");
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private boolean looksLikeUrl(String value) {
        return value.startsWith("http://") || value.startsWith("https://");
    }

    private boolean looksLikeImageUrl(String value) {
        return looksLikeUrl(value) && value.matches("(?i).+\\.(png|jpg|jpeg|webp|gif)(\\?.*)?$");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record PathSample(String path, List<String> values, int averageLength) {

        private String firstValue() {
            return values.isEmpty() ? "" : values.get(0);
        }
    }

    private static final class PathSampleBuilder {

        private final String path;
        private final List<String> values = new ArrayList<>();

        private PathSampleBuilder(String path) {
            this.path = path;
        }

        private void add(String value) {
            if (values.size() < 3) {
                values.add(value.strip());
            }
        }

        private PathSample build() {
            int averageLength = values.stream()
                    .mapToInt(String::length)
                    .sum() / Math.max(1, values.size());
            return new PathSample(path, List.copyOf(values), averageLength);
        }
    }

    private record ScoredPath(String path, int score) {
    }

    private record DeepSeekRequest(
            String model,
            List<DeepSeekMessage> messages,
            ResponseFormat response_format,
            double temperature) {
    }

    private record DeepSeekMessage(String role, String content) {
    }

    private record ResponseFormat(String type) {
    }

    private record DeepSeekResponse(List<DeepSeekChoice> choices) {
    }

    private record DeepSeekChoice(DeepSeekMessage message) {
    }
}
