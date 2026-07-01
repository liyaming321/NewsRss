package com.newsrss.common;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 统一错误响应。
 *
 * @param status HTTP 状态码
 * @param message 可读错误信息
 * @param details 详细错误列表
 * @param path 请求路径
 * @param checkedAt 响应生成时间
 */
public record ApiErrorResponse(
        int status,
        String message,
        List<String> details,
        String path,
        OffsetDateTime checkedAt) {
}
