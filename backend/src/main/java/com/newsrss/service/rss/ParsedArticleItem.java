package com.newsrss.service.rss;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.OffsetDateTime;

/**
 * 标准化后的 RSS 文章条目，用于从解析层传递到入库层。
 *
 * @param guid RSS 条目 GUID
 * @param articleUrl 文章链接
 * @param title 文章标题
 * @param summary 文章摘要
 * @param contentHtml 清洗后的正文 HTML
 * @param author 作者
 * @param publishedAt 发布时间
 * @param coverImageUrl 封面图地址
 * @param fingerprint 去重指纹
 * @param rawPayload 原始条目数据
 * @param parseTrace 解析命中轨迹
 * @param customFields 自定义字段值
 * @param readingMinutes 预计阅读分钟数
 * @param wordCount 文章字数
 */
public record ParsedArticleItem(
        String guid,
        String articleUrl,
        String title,
        String summary,
        String contentHtml,
        String author,
        OffsetDateTime publishedAt,
        String coverImageUrl,
        String fingerprint,
        JsonNode rawPayload,
        JsonNode parseTrace,
        JsonNode customFields,
        Integer readingMinutes,
        Integer wordCount) {
}
