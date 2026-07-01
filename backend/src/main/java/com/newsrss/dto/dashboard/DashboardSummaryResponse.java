package com.newsrss.dto.dashboard;

/**
 * 驾驶舱摘要响应。
 *
 * @param feedCount 订阅源数量
 * @param enabledFeedCount 启用订阅源数量
 * @param articleCount 文章数量
 * @param todayNewArticleCount 今日新增文章数量
 * @param unreadCount 未读数量
 * @param favoriteCount 收藏数量
 * @param readLaterCount 稍后读数量
 * @param failedFetchLogCount 失败抓取日志数量
 */
public record DashboardSummaryResponse(
        long feedCount,
        long enabledFeedCount,
        long articleCount,
        long todayNewArticleCount,
        long unreadCount,
        long favoriteCount,
        long readLaterCount,
        long failedFetchLogCount) {
}
