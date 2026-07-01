package com.newsrss.dto.feed;

import jakarta.validation.constraints.NotBlank;

/**
 * 订阅源探测请求。
 *
 * @param feedUrl RSS 或 Atom 地址
 */
public record FeedDetectRequest(
        @NotBlank(message = "RSS 地址不能为空")
        String feedUrl) {
}
