package com.newsrss.dto.article;

import java.util.List;

/**
 * 文章列表统计响应。
 *
 * @param totalCount 全部文章数量
 * @param unreadCount 未读文章数量
 * @param favoriteCount 收藏文章数量
 * @param readLaterCount 稍后读文章数量
 * @param todayCount 今日抓取文章数量
 * @param feedStats 订阅源文章数量
 */
public record ArticleStatsResponse(
        long totalCount,
        long unreadCount,
        long favoriteCount,
        long readLaterCount,
        long todayCount,
        List<ArticleFeedStatResponse> feedStats) {
}
