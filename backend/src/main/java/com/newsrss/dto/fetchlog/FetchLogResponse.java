package com.newsrss.dto.fetchlog;

import java.time.OffsetDateTime;

/**
 * 抓取日志响应。
 *
 * @param id 日志主键
 * @param feedId 订阅源主键
 * @param feedName 订阅源名称
 * @param startedAt 开始时间
 * @param finishedAt 结束时间
 * @param status 状态
 * @param requestUrl 请求地址
 * @param httpStatus HTTP 状态码
 * @param fetchedCount 抓取条数
 * @param newCount 新增条数
 * @param duplicateCount 重复条数
 * @param failedCount 失败条数
 * @param durationMs 耗时毫秒
 * @param errorMessage 错误信息
 * @param errorStack 错误堆栈
 * @param rawResponseSample 原始响应样本
 * @param createdAt 创建时间
 */
public record FetchLogResponse(
        Long id,
        Long feedId,
        String feedName,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        String status,
        String requestUrl,
        Integer httpStatus,
        Integer fetchedCount,
        Integer newCount,
        Integer duplicateCount,
        Integer failedCount,
        Long durationMs,
        String errorMessage,
        String errorStack,
        String rawResponseSample,
        OffsetDateTime createdAt) {
}
