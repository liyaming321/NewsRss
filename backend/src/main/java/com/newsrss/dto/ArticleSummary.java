package com.newsrss.dto;

import java.time.OffsetDateTime;

/**
 * 文章摘要 DTO，用于文章列表和驾驶舱最近文章模块。
 *
 * @param id 文章主键
 * @param feedId 订阅源主键
 * @param title 文章标题
 * @param articleUrl 文章链接
 * @param publishedAt 发布时间
 * @param coverImageUrl 封面图地址
 */
public record ArticleSummary(
        Long id,
        Long feedId,
        String title,
        String articleUrl,
        OffsetDateTime publishedAt,
        String coverImageUrl) {}
