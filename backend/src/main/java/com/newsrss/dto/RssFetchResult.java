package com.newsrss.dto;

/**
 * RSS 抓取结果 DTO，用于命令行验证和后续接口响应。
 *
 * @param feedUrl 订阅地址
 * @param success 是否抓取成功
 * @param fetchedCount 抓取条目数
 * @param newCount 新增文章数
 * @param duplicateCount 重复文章数
 * @param failedCount 失败条目数
 * @param errorMessage 失败时的可读错误信息
 */
public record RssFetchResult(
        String feedUrl,
        boolean success,
        int fetchedCount,
        int newCount,
        int duplicateCount,
        int failedCount,
        String errorMessage) {

    /**
     * 创建成功抓取结果。
     *
     * @param feedUrl 订阅地址
     * @param fetchedCount 抓取条目数
     * @param newCount 新增文章数
     * @param duplicateCount 重复文章数
     * @param failedCount 失败条目数
     * @return 成功结果
     */
    public static RssFetchResult success(
            String feedUrl,
            int fetchedCount,
            int newCount,
            int duplicateCount,
            int failedCount) {
        return new RssFetchResult(feedUrl, true, fetchedCount, newCount, duplicateCount, failedCount, null);
    }

    /**
     * 创建失败抓取结果。
     *
     * @param feedUrl 订阅地址
     * @param errorMessage 失败时的可读错误信息
     * @return 失败结果
     */
    public static RssFetchResult failure(String feedUrl, String errorMessage) {
        return new RssFetchResult(feedUrl, false, 0, 0, 0, 0, errorMessage);
    }
}
