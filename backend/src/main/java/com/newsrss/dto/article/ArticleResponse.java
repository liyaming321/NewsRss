package com.newsrss.dto.article;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.OffsetDateTime;

/**
 * 文章响应。
 *
 * @param id 文章主键
 * @param feedId 订阅源主键
 * @param feedName 订阅源名称
 * @param guid RSS 条目 GUID
 * @param articleUrl 文章链接
 * @param canonicalUrl 规范化链接
 * @param title 标题
 * @param summary 摘要
 * @param contentHtml 正文 HTML
 * @param author 作者
 * @param publishedAt 发布时间
 * @param fetchedAt 抓取时间
 * @param coverImageUrl 封面图
 * @param readingMinutes 预计阅读分钟
 * @param wordCount 字数
 * @param rawPayload 原始字段
 * @param parseTrace 解析轨迹
 * @param customFields 自定义字段
 * @param state 阅读状态
 */
public record ArticleResponse(
        Long id,
        Long feedId,
        String feedName,
        String guid,
        String articleUrl,
        String canonicalUrl,
        String title,
        String summary,
        String contentHtml,
        String author,
        OffsetDateTime publishedAt,
        OffsetDateTime fetchedAt,
        String coverImageUrl,
        Integer readingMinutes,
        Integer wordCount,
        JsonNode rawPayload,
        JsonNode parseTrace,
        JsonNode customFields,
        ArticleStateResponse state) {
}
