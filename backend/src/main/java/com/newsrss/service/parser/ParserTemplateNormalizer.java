package com.newsrss.service.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.newsrss.service.rss.ParsedArticleItem;
import com.newsrss.service.rss.RssContentCleaner;
import com.newsrss.service.rss.RssFingerprintGenerator;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 解析模板标准化器，按模板路径和候选优先级生成系统标准文章结构。
 */
@Component
@Profile("db")
public class ParserTemplateNormalizer {

    private static final Map<String, List<String>> DEFAULT_FIELD_PATHS = Map.of(
            "guid", List.of("uri"),
            "articleUrl", List.of("link", "links[0].href"),
            "title", List.of("title"),
            "summary", List.of("description.value", "description.text"),
            "author", List.of("author", "authors[0].name"),
            "publishedAt", List.of("publishedDate", "updatedDate"));

    private final ObjectMapper objectMapper;
    private final RssContentCleaner contentCleaner;
    private final RssFingerprintGenerator fingerprintGenerator;

    public ParserTemplateNormalizer(
            ObjectMapper objectMapper,
            RssContentCleaner contentCleaner,
            RssFingerprintGenerator fingerprintGenerator) {
        this.objectMapper = objectMapper;
        this.contentCleaner = contentCleaner;
        this.fingerprintGenerator = fingerprintGenerator;
    }

    /**
     * 按模板配置标准化单条文章。
     *
     * @param feedUrl RSS 或 Atom 订阅地址
     * @param rawPayload 原始字段树
     * @param config 模板配置
     * @return 标准化结果
     */
    public TemplateNormalizedArticle normalize(String feedUrl, JsonNode rawPayload, ParserTemplateConfig config) {
        Map<String, TemplateFieldHit> fieldHits = new LinkedHashMap<>();
        Map<String, TemplateFieldHit> customFieldHits = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();

        FieldValue guid = resolveField(rawPayload, config, "guid", DEFAULT_FIELD_PATHS.get("guid"));
        FieldValue articleUrl = resolveField(rawPayload, config, "articleUrl", DEFAULT_FIELD_PATHS.get("articleUrl"));
        FieldValue title = resolveField(rawPayload, config, "title", DEFAULT_FIELD_PATHS.get("title"));
        FieldValue summary = resolveField(rawPayload, config, "summary", DEFAULT_FIELD_PATHS.get("summary"));
        FieldValue author = resolveField(rawPayload, config, "author", DEFAULT_FIELD_PATHS.get("author"));
        FieldValue publishedAtText = resolveField(rawPayload, config, "publishedAt", DEFAULT_FIELD_PATHS.get("publishedAt"));
        FieldValue rawContent = resolveCandidate(
                rawPayload,
                mergeCandidates(
                        mergeCandidates(config.fieldMapping().getOrDefault("contentHtml", List.of()), config.contentSelectors()),
                        List.of("contents[0].value", "description.value")),
                "contentHtml",
                mergeCandidates(config.fieldMapping().getOrDefault("contentHtml", List.of()), config.contentSelectors()));
        FieldValue coverImage = resolveCandidate(
                rawPayload,
                mergeCandidates(
                        mergeCandidates(config.fieldMapping().getOrDefault("coverImageUrl", List.of()), config.coverSelectors()),
                        List.of("enclosures[0].url")),
                "coverImageUrl",
                mergeCandidates(config.fieldMapping().getOrDefault("coverImageUrl", List.of()), config.coverSelectors()));

        String articleUrlText = requireValue(articleUrl, "articleUrl", warnings);
        String titleText = normalizeBlank(title.value());
        if (titleText == null) {
            titleText = "未命名文章";
            warnings.add("title 字段缺失，已使用默认标题");
        }
        String cleanedContent = cleanContent(rawContent.value(), config.cleanupRules());
        String coverImageUrl = normalizeBlank(coverImage.value());
        if (coverImageUrl == null) {
            coverImageUrl = contentCleaner.firstImageUrl(cleanedContent);
            if (coverImageUrl != null) {
                coverImage = coverImage.withFallback("contentHtml.img", coverImageUrl, "封面图未命中模板字段，已从正文图片兜底");
            }
        }
        OffsetDateTime publishedAt = parsePublishedAt(publishedAtText.value(), config.timeFormats(), warnings);
        String plainText = contentCleaner.toPlainText(cleanedContent != null ? cleanedContent : summary.value());
        int wordCount = countWords(plainText);
        int readingMinutes = Math.max(1, (int) Math.ceil(wordCount / 400.0));
        String publishedAtFingerprintText = publishedAt == null ? publishedAtText.value() : publishedAt.toString();
        String fingerprint = fingerprintGenerator.generate(
                feedUrl,
                guid.value(),
                articleUrlText,
                titleText,
                publishedAtFingerprintText);

        addFieldHit(fieldHits, guid);
        addFieldHit(fieldHits, articleUrl);
        addFieldHit(fieldHits, title);
        addFieldHit(fieldHits, summary);
        addFieldHit(fieldHits, author);
        addFieldHit(fieldHits, publishedAtText);
        addFieldHit(fieldHits, rawContent);
        addFieldHit(fieldHits, coverImage);

        ObjectNode customFields = resolveCustomFields(rawPayload, config, customFieldHits);
        JsonNode parseTrace = buildParseTrace(config, fieldHits, customFieldHits, warnings);
        ParsedArticleItem item = new ParsedArticleItem(
                normalizeBlank(guid.value()),
                articleUrlText,
                titleText,
                normalizeBlank(summary.value()),
                cleanedContent,
                normalizeBlank(author.value()),
                publishedAt,
                coverImageUrl,
                fingerprint,
                rawPayload,
                parseTrace,
                customFields,
                readingMinutes,
                wordCount);
        return new TemplateNormalizedArticle(item, rawPayload, Map.copyOf(fieldHits), Map.copyOf(customFieldHits), List.copyOf(warnings));
    }

    /**
     * 解析标准字段，优先使用模板配置路径，再使用默认路径兜底。
     *
     * @param rawPayload 原始字段树
     * @param config 模板配置
     * @param fieldName 标准字段名称
     * @param defaultPaths 默认候选路径
     * @return 字段命中结果
     */
    private FieldValue resolveField(
            JsonNode rawPayload,
            ParserTemplateConfig config,
            String fieldName,
            List<String> defaultPaths) {
        List<String> configuredPaths = config.fieldMapping().getOrDefault(fieldName, List.of());
        List<String> paths = mergeCandidates(configuredPaths, defaultPaths);
        return resolveCandidate(rawPayload, paths, fieldName, configuredPaths);
    }

    /**
     * 按候选路径顺序解析字段值。
     *
     * @param rawPayload 原始字段树
     * @param paths 候选路径
     * @param fieldName 标准字段名称
     * @param configuredPaths 模板显式配置路径
     * @return 字段命中结果
     */
    private FieldValue resolveCandidate(
            JsonNode rawPayload,
            List<String> paths,
            String fieldName,
            List<String> configuredPaths) {
        for (String path : paths) {
            JsonNode node = resolvePath(rawPayload, path);
            String value = textValue(node);
            if (value != null) {
                boolean fallback = !configuredPaths.isEmpty() && !configuredPaths.contains(path);
                return new FieldValue(fieldName, true, path, value, fallback, fallback ? "模板字段未命中，已使用默认字段兜底" : null);
            }
        }
        return new FieldValue(fieldName, false, null, null, false, "未命中可用字段");
    }

    /**
     * 合并模板候选路径和默认候选路径，并保留优先级顺序。
     *
     * @param configuredPaths 模板显式配置路径
     * @param defaultPaths 默认候选路径
     * @return 合并后的候选路径
     */
    private List<String> mergeCandidates(List<String> configuredPaths, List<String> defaultPaths) {
        List<String> paths = new ArrayList<>();
        paths.addAll(configuredPaths);
        defaultPaths.stream()
                .filter(path -> !paths.contains(path))
                .forEach(paths::add);
        return List.copyOf(paths);
    }

    /**
     * 按点号路径读取 JSON 节点，支持 `items[0].value` 形式的数组下标。
     *
     * @param rootNode 根节点
     * @param path 字段路径
     * @return 命中的 JSON 节点，未命中时返回 null
     */
    private JsonNode resolvePath(JsonNode rootNode, String path) {
        if (rootNode == null || path == null || path.isBlank()) {
            return null;
        }
        JsonNode currentNode = rootNode;
        for (PathToken token : parsePath(path)) {
            if (currentNode == null || currentNode.isMissingNode() || currentNode.isNull()) {
                return null;
            }
            currentNode = currentNode.get(token.name());
            if (token.index() != null) {
                if (currentNode == null || !currentNode.isArray() || currentNode.size() <= token.index()) {
                    return null;
                }
                currentNode = currentNode.get(token.index());
            }
        }
        return currentNode;
    }

    /**
     * 将字段路径拆分为可执行的路径 token。
     *
     * @param path 字段路径
     * @return 路径 token 列表
     */
    private List<PathToken> parsePath(String path) {
        List<PathToken> tokens = new ArrayList<>();
        for (String part : path.split("\\.")) {
            String tokenName = part;
            Integer index = null;
            int bracketStart = part.indexOf('[');
            int bracketEnd = part.indexOf(']');
            if (bracketStart > 0 && bracketEnd > bracketStart) {
                tokenName = part.substring(0, bracketStart);
                index = Integer.parseInt(part.substring(bracketStart + 1, bracketEnd));
            }
            tokens.add(new PathToken(tokenName, index));
        }
        return tokens;
    }

    /**
     * 从 JSON 节点中提取文本值。
     *
     * @param node JSON 节点
     * @return 文本值，未命中时返回 null
     */
    private String textValue(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            return normalizeBlank(node.asText());
        }
        if (node.isNumber() || node.isBoolean()) {
            return node.asText();
        }
        if (node.isObject()) {
            String value = textValue(node.get("value"));
            if (value != null) {
                return value;
            }
            return textValue(node.get("text"));
        }
        return null;
    }

    /**
     * 获取必填字段文本，缺失时添加预览提示并生成占位值。
     *
     * @param value 字段命中结果
     * @param fieldName 标准字段名称
     * @param warnings 异常提示列表
     * @return 字段文本或占位值
     */
    private String requireValue(FieldValue value, String fieldName, List<String> warnings) {
        String text = normalizeBlank(value.value());
        if (text == null) {
            warnings.add(fieldName + " 字段缺失，当前条目无法入库，只能用于预览排查");
            return "missing://" + fieldName + "/" + System.nanoTime();
        }
        return text;
    }

    /**
     * 解析发布时间，优先使用 ISO 时间，再尝试模板自定义格式。
     *
     * @param value 发布时间文本
     * @param timeFormats 自定义时间格式列表
     * @param warnings 异常提示列表
     * @return 发布时间，解析失败时返回 null
     */
    private OffsetDateTime parsePublishedAt(String value, List<String> timeFormats, List<String> warnings) {
        String text = normalizeBlank(value);
        if (text == null) {
            warnings.add("publishedAt 字段缺失，发布时间为空");
            return null;
        }
        try {
            return OffsetDateTime.parse(text);
        } catch (DateTimeParseException ignored) {
            // 继续使用自定义格式解析。
        }
        for (String timeFormat : timeFormats) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(timeFormat);
                return LocalDateTime.parse(text, formatter).atOffset(ZoneOffset.UTC);
            } catch (DateTimeParseException exception) {
                // 当前格式不匹配时继续尝试后续格式。
            }
        }
        warnings.add("publishedAt 时间解析失败：" + text);
        return null;
    }

    /**
     * 按模板清洗规则处理正文 HTML。
     *
     * @param html 原始 HTML
     * @param cleanupRules 清洗规则
     * @return 清洗后的安全 HTML
     */
    private String cleanContent(String html, ParserTemplateConfig.CleanupRules cleanupRules) {
        String normalizedHtml = normalizeBlank(html);
        if (normalizedHtml == null) {
            return null;
        }
        Document document = Jsoup.parseBodyFragment(normalizedHtml);
        cleanupRules.removeSelectors().forEach(selector -> document.select(selector).remove());
        cleanupRules.unwrapSelectors().forEach(selector -> document.select(selector).unwrap());
        cleanupRules.removeAttributes().forEach(attribute -> document.getAllElements().removeAttr(attribute));
        return contentCleaner.clean(document.body().html());
    }

    /**
     * 写入字段命中轨迹。
     *
     * @param fieldHits 字段命中轨迹集合
     * @param value 字段命中结果
     */
    private void addFieldHit(Map<String, TemplateFieldHit> fieldHits, FieldValue value) {
        fieldHits.put(
                value.fieldName(),
                new TemplateFieldHit(
                        value.fieldName(),
                        value.matched(),
                        value.path(),
                        value.value(),
                        value.fallback(),
                        value.message()));
    }

    /**
     * 解析模板中的自定义字段映射。
     *
     * @param rawPayload 原始字段树
     * @param config 模板配置
     * @param customFieldHits 自定义字段命中轨迹
     * @return 自定义字段值
     */
    private ObjectNode resolveCustomFields(
            JsonNode rawPayload,
            ParserTemplateConfig config,
            Map<String, TemplateFieldHit> customFieldHits) {
        ObjectNode customFields = objectMapper.createObjectNode();
        config.customFieldMapping().forEach((fieldName, paths) -> {
            FieldValue value = resolveCandidate(rawPayload, paths, fieldName, paths);
            customFieldHits.put(
                    fieldName,
                    new TemplateFieldHit(
                            fieldName,
                            value.matched(),
                            value.path(),
                            value.value(),
                            value.fallback(),
                            value.message()));
            if (value.matched()) {
                customFields.put(fieldName, value.value());
            }
        });
        return customFields;
    }

    /**
     * 构建入库用解析轨迹 JSON。
     *
     * @param config 模板配置
     * @param fieldHits 字段命中轨迹
     * @param warnings 异常提示
     * @return 解析轨迹 JSON
     */
    private JsonNode buildParseTrace(
            ParserTemplateConfig config,
            Map<String, TemplateFieldHit> fieldHits,
            Map<String, TemplateFieldHit> customFieldHits,
            List<String> warnings) {
        ObjectNode trace = objectMapper.createObjectNode();
        trace.put("parser", "template");
        trace.put("templateCode", config.templateCode());
        ObjectNode fieldsNode = objectMapper.createObjectNode();
        fieldHits.forEach((fieldName, hit) -> {
            ObjectNode hitNode = objectMapper.createObjectNode();
            hitNode.put("matched", hit.matched());
            hitNode.put("path", hit.path());
            hitNode.put("fallback", hit.fallback());
            hitNode.put("message", hit.message());
            fieldsNode.set(fieldName, hitNode);
        });
        trace.set("fields", fieldsNode);
        ObjectNode customFieldsNode = objectMapper.createObjectNode();
        customFieldHits.forEach((fieldName, hit) -> {
            ObjectNode hitNode = objectMapper.createObjectNode();
            hitNode.put("matched", hit.matched());
            hitNode.put("path", hit.path());
            hitNode.put("message", hit.message());
            customFieldsNode.set(fieldName, hitNode);
        });
        trace.set("customFields", customFieldsNode);
        trace.set("warnings", objectMapper.valueToTree(warnings));
        return trace;
    }

    /**
     * 估算正文词数，中文按非空白字符计数，其他语言按空白切词。
     *
     * @param text 正文纯文本
     * @return 估算词数
     */
    private int countWords(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        String compactText = text.strip();
        if (compactText.chars().anyMatch(character -> Character.UnicodeScript.of(character) == Character.UnicodeScript.HAN)) {
            return (int) compactText.chars()
                    .filter(character -> !Character.isWhitespace(character))
                    .count();
        }
        return compactText.split("\\s+").length;
    }

    /**
     * 将空白字符串统一归一为空值。
     *
     * @param value 原始字符串
     * @return 去除首尾空白后的字符串，空白时返回 null
     */
    private String normalizeBlank(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }

    private record PathToken(String name, Integer index) {
    }

    private record FieldValue(
            String fieldName,
            boolean matched,
            String path,
            String value,
            boolean fallback,
            String message) {

        private FieldValue withFallback(String path, String value, String message) {
            return new FieldValue(fieldName, true, path, value, true, message);
        }
    }
}
