package com.newsrss.service.parser;

import com.newsrss.domain.entity.RssParserTemplate;
import com.newsrss.dto.parser.ParserTemplatePreviewRequest;
import com.newsrss.dto.parser.ParserTemplatePreviewResponse;
import com.newsrss.dto.parser.ParserTemplatePreviewResponse.FieldHit;
import com.newsrss.dto.parser.ParserTemplatePreviewResponse.NormalizedArticle;
import com.newsrss.dto.parser.ParserTemplatePreviewResponse.PreviewItem;
import com.newsrss.service.rss.ParsedFeed;
import com.newsrss.service.rss.RssFeedParser;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * 解析模板预览服务，负责抓取远端 Feed 并返回模板命中效果。
 */
@Service
@Profile("db")
public class ParserTemplatePreviewService {

    private static final int DEFAULT_PREVIEW_LIMIT = 5;
    private static final int MAX_PREVIEW_LIMIT = 10;

    private final ParserTemplateManagementService managementService;
    private final ParserTemplateConfigMapper configMapper;
    private final RssFeedParser feedParser;

    public ParserTemplatePreviewService(
            ParserTemplateManagementService managementService,
            ParserTemplateConfigMapper configMapper,
            RssFeedParser feedParser) {
        this.managementService = managementService;
        this.configMapper = configMapper;
        this.feedParser = feedParser;
    }

    /**
     * 预览模板解析结果。
     *
     * @param request 预览请求
     * @return 预览响应
     */
    public ParserTemplatePreviewResponse preview(ParserTemplatePreviewRequest request) {
        ParserTemplateConfig config = resolveConfig(request);
        if (!config.enabled()) {
            throw new IllegalArgumentException("解析模板未启用，不能预览");
        }
        int limit = normalizeLimit(request.limit());
        ParsedFeed parsedFeed = feedParser.parse(request.feedUrl(), config);
        List<TemplateNormalizedArticle> normalizedArticles = parsedFeed.templateArticles().stream()
                .limit(limit)
                .toList();
        List<PreviewItem> items = new ArrayList<>();
        for (int index = 0; index < normalizedArticles.size(); index++) {
            items.add(toPreviewItem(index + 1, normalizedArticles.get(index)));
        }
        Map<String, Double> fieldHitRates = calculateFieldHitRates(normalizedArticles);
        List<String> warnings = normalizedArticles.stream()
                .flatMap(article -> article.warnings().stream())
                .distinct()
                .toList();
        return new ParserTemplatePreviewResponse(
                request.feedUrl(),
                parsedFeed.title(),
                config.templateCode(),
                limit,
                items.size(),
                calculateOverallHitRate(fieldHitRates),
                fieldHitRates,
                warnings,
                items);
    }

    /**
     * 解析预览使用的模板配置，优先使用已保存模板，其次使用请求内联模板。
     *
     * @param request 预览请求
     * @return 运行期模板配置
     */
    private ParserTemplateConfig resolveConfig(ParserTemplatePreviewRequest request) {
        if (request.templateId() != null) {
            RssParserTemplate template = managementService.findTemplate(request.templateId());
            return configMapper.fromEntity(template);
        }
        ParserTemplatePreviewRequest.PreviewTemplatePayload payload = request.template();
        if (payload == null) {
            return configMapper.fromJson(
                    "default-preview",
                    "默认预览模板",
                    configMapper.emptyObject(),
                    configMapper.emptyObject(),
                    configMapper.emptyArray(),
                    configMapper.emptyArray(),
                    configMapper.emptyArray(),
                    configMapper.emptyObject(),
                    true);
        }
        return configMapper.fromJson(
                payload.templateCode(),
                payload.templateName(),
                payload.fieldMapping(),
                payload.customFieldMapping(),
                payload.contentSelectors(),
                payload.coverSelectors(),
                payload.timeFormats(),
                payload.cleanupRules(),
                payload.enabled() == null || payload.enabled());
    }

    /**
     * 归一化预览条数，限制单次预览上限。
     *
     * @param limit 请求条数
     * @return 实际预览条数
     */
    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_PREVIEW_LIMIT;
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("预览条数必须大于 0");
        }
        return Math.min(limit, MAX_PREVIEW_LIMIT);
    }

    /**
     * 将模板标准化结果转换为接口预览条目。
     *
     * @param index 条目序号
     * @param article 模板标准化结果
     * @return 接口预览条目
     */
    private PreviewItem toPreviewItem(int index, TemplateNormalizedArticle article) {
        Map<String, FieldHit> fieldHits = new LinkedHashMap<>();
        article.fieldHits().forEach((fieldName, hit) -> fieldHits.put(
                fieldName,
                new FieldHit(hit.matched(), hit.path(), hit.value(), hit.fallback(), hit.message())));
        Map<String, FieldHit> customFieldHits = new LinkedHashMap<>();
        article.customFieldHits().forEach((fieldName, hit) -> customFieldHits.put(
                fieldName,
                new FieldHit(hit.matched(), hit.path(), hit.value(), hit.fallback(), hit.message())));
        return new PreviewItem(
                index,
                article.rawPayload(),
                new NormalizedArticle(
                        article.item().guid(),
                        article.item().articleUrl(),
                        article.item().title(),
                        article.item().summary(),
                        article.item().contentHtml(),
                        article.item().customFields(),
                        article.item().author(),
                        article.item().publishedAt(),
                        article.item().coverImageUrl(),
                        article.item().readingMinutes(),
                        article.item().wordCount()),
                fieldHits,
                customFieldHits,
                article.warnings());
    }

    /**
     * 计算每个标准字段的命中率。
     *
     * @param articles 模板标准化结果列表
     * @return 字段命中率
     */
    private Map<String, Double> calculateFieldHitRates(List<TemplateNormalizedArticle> articles) {
        Map<String, Integer> totalByField = new LinkedHashMap<>();
        Map<String, Integer> hitByField = new LinkedHashMap<>();
        articles.forEach(article -> article.fieldHits().forEach((fieldName, hit) -> {
            totalByField.merge(fieldName, 1, Integer::sum);
            if (hit.matched()) {
                hitByField.merge(fieldName, 1, Integer::sum);
            }
        }));
        Map<String, Double> rates = new LinkedHashMap<>();
        totalByField.forEach((fieldName, total) -> {
            int hit = hitByField.getOrDefault(fieldName, 0);
            rates.put(fieldName, roundRate((double) hit / total));
        });
        return rates;
    }

    /**
     * 计算整体字段命中率。
     *
     * @param fieldHitRates 字段命中率
     * @return 整体命中率
     */
    private double calculateOverallHitRate(Map<String, Double> fieldHitRates) {
        if (fieldHitRates.isEmpty()) {
            return 0;
        }
        double sum = fieldHitRates.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();
        return roundRate(sum / fieldHitRates.size());
    }

    /**
     * 将命中率保留四位小数。
     *
     * @param value 原始命中率
     * @return 四位小数命中率
     */
    private double roundRate(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }
}
