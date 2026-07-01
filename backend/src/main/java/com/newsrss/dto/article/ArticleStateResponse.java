package com.newsrss.dto.article;

import java.time.OffsetDateTime;

/**
 * 文章阅读状态响应。
 *
 * @param read 是否已读
 * @param favorite 是否收藏
 * @param readLater 是否稍后读
 * @param archived 是否归档
 * @param readAt 已读时间
 * @param favoritedAt 收藏时间
 * @param readLaterAt 稍后读时间
 * @param archivedAt 归档时间
 */
public record ArticleStateResponse(
        boolean read,
        boolean favorite,
        boolean readLater,
        boolean archived,
        OffsetDateTime readAt,
        OffsetDateTime favoritedAt,
        OffsetDateTime readLaterAt,
        OffsetDateTime archivedAt) {
}
