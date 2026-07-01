package com.newsrss.dto.common;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * 统一 API 成功响应。
 *
 * @param success 是否成功
 * @param message 可读消息
 * @param data 响应数据
 * @param checkedAt 响应生成时间
 * @param <T> 响应数据类型
 */
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        OffsetDateTime checkedAt) {

    /**
     * 创建成功响应。
     *
     * @param data 响应数据
     * @param <T> 响应数据类型
     * @return 统一成功响应
     */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "OK", data, OffsetDateTime.now(ZoneOffset.UTC));
    }

    /**
     * 创建带消息的成功响应。
     *
     * @param message 可读消息
     * @param data 响应数据
     * @param <T> 响应数据类型
     * @return 统一成功响应
     */
    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data, OffsetDateTime.now(ZoneOffset.UTC));
    }
}
