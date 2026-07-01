package com.newsrss.dto.feed;

/**
 * 订阅源探测响应。
 *
 * @param feedUrl RSS 或 Atom 地址
 * @param title Feed 标题
 * @param siteUrl 站点地址
 * @param description Feed 说明
 * @param language 语言代码
 * @param itemCount 条目数量
 */
public record FeedDetectResponse(
        String feedUrl,
        String title,
        String siteUrl,
        String description,
        String language,
        int itemCount) {
}
