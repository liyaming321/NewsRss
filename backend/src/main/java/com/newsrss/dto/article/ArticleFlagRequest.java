package com.newsrss.dto.article;

import jakarta.validation.constraints.NotNull;

/**
 * 文章布尔状态更新请求。
 *
 * @param value 状态值
 */
public record ArticleFlagRequest(
        @NotNull(message = "状态值不能为空")
        Boolean value) {
}
