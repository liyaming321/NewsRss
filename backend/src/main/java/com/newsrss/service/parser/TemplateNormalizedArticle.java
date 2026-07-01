package com.newsrss.service.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.newsrss.service.rss.ParsedArticleItem;
import java.util.List;
import java.util.Map;

/**
 * 模板标准化文章结果。
 *
 * @param item 标准化文章条目
 * @param rawPayload 原始字段快照
 * @param fieldHits 字段命中结果
 * @param customFieldHits 自定义字段命中结果
 * @param warnings 异常提示
 */
public record TemplateNormalizedArticle(
        ParsedArticleItem item,
        JsonNode rawPayload,
        Map<String, TemplateFieldHit> fieldHits,
        Map<String, TemplateFieldHit> customFieldHits,
        List<String> warnings) {
}
