package com.newsrss.dto.common;

import java.util.List;

/**
 * 分页响应。
 *
 * @param items 当前页数据
 * @param page 页码，从 0 开始
 * @param size 每页条数
 * @param totalElements 总记录数
 * @param totalPages 总页数
 * @param <T> 列表项类型
 */
public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
