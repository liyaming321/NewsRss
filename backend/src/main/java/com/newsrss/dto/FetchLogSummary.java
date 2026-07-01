package com.newsrss.dto;

import java.time.OffsetDateTime;

/**
 * 抓取日志摘要 DTO，用于抓取日志列表。
 *
 * @param id 抓取日志主键
 * @param feedId 订阅源主键
 * @param status 抓取状态
 * @param startedAt 抓取开始时间
 * @param finishedAt 抓取结束时间
 * @param fetchedCount 抓取条目数
 * @param newCount 新增文章数
 */
public record FetchLogSummary(
        Long id,
        Long feedId,
        String status,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        int fetchedCount,
        int newCount) {}
