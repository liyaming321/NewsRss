package com.newsrss.dto.feed;

import java.util.List;

/**
 * 批量刷新请求。
 *
 * @param feedIds 指定订阅源主键列表，为空时刷新全部启用源
 */
public record FeedBatchRefreshRequest(List<Long> feedIds) {
}
