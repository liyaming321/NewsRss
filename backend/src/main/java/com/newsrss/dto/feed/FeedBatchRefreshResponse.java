package com.newsrss.dto.feed;

import com.newsrss.dto.RssFetchResult;
import java.util.List;

/**
 * 批量刷新响应。
 *
 * @param requestedCount 请求刷新数量
 * @param successCount 成功数量
 * @param failureCount 失败数量
 * @param results 抓取结果列表
 */
public record FeedBatchRefreshResponse(
        int requestedCount,
        int successCount,
        int failureCount,
        List<RssFetchResult> results) {
}
