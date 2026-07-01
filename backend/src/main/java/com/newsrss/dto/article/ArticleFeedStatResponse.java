package com.newsrss.dto.article;

/**
 * 订阅源文章统计响应。
 *
 * @param feedId 订阅源主键
 * @param feedName 订阅源名称
 * @param count 文章数量
 */
public record ArticleFeedStatResponse(
        Long feedId,
        String feedName,
        long count) {
}
