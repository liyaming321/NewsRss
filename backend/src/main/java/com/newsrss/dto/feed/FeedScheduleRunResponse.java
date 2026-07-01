package com.newsrss.dto.feed;

import com.newsrss.dto.RssFetchResult;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 调度执行响应。
 *
 * @param triggeredAt 触发时间
 * @param dueCount 本次到期源数量
 * @param successCount 成功数量
 * @param failureCount 失败数量
 * @param results 抓取结果列表
 */
public record FeedScheduleRunResponse(
        OffsetDateTime triggeredAt,
        int dueCount,
        int successCount,
        int failureCount,
        List<RssFetchResult> results) {
}
