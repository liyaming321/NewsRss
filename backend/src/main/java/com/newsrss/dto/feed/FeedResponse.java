package com.newsrss.dto.feed;

import java.time.OffsetDateTime;

/**
 * 订阅源响应。
 *
 * @param id 订阅源主键
 * @param parserTemplateId 解析模板主键
 * @param parserTemplateCode 解析模板编码
 * @param feedName 订阅源名称
 * @param feedUrl RSS 或 Atom 地址
 * @param siteUrl 站点地址
 * @param description 订阅源说明
 * @param language 语言代码
 * @param category 分类
 * @param iconUrl 图标地址
 * @param healthStatus 健康状态
 * @param enabled 是否启用
 * @param fetchIntervalMinutes 抓取间隔分钟数
 * @param lastFetchAt 最近抓取时间
 * @param nextFetchAt 下次抓取时间
 * @param lastSuccessAt 最近成功时间
 * @param lastFailureAt 最近失败时间
 * @param consecutiveFailureCount 连续失败次数
 * @param articleCount 文章数量
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record FeedResponse(
        Long id,
        Long parserTemplateId,
        String parserTemplateCode,
        String feedName,
        String feedUrl,
        String siteUrl,
        String description,
        String language,
        String category,
        String iconUrl,
        String healthStatus,
        boolean enabled,
        Integer fetchIntervalMinutes,
        OffsetDateTime lastFetchAt,
        OffsetDateTime nextFetchAt,
        OffsetDateTime lastSuccessAt,
        OffsetDateTime lastFailureAt,
        Integer consecutiveFailureCount,
        long articleCount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
