package com.newsrss.dto.dashboard;

/**
 * 订阅源健康状态统计响应。
 *
 * @param unknown 未知数量
 * @param healthy 健康数量
 * @param warning 警告数量
 * @param error 错误数量
 */
public record FeedHealthResponse(
        long unknown,
        long healthy,
        long warning,
        long error) {
}
