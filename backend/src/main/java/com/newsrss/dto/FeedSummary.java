package com.newsrss.dto;

/**
 * 订阅源摘要 DTO，用于订阅源列表和驾驶舱模块。
 *
 * @param id 订阅源主键
 * @param feedName 订阅源名称
 * @param feedUrl 订阅地址
 * @param healthStatus 健康状态
 * @param enabled 是否启用
 */
public record FeedSummary(Long id, String feedName, String feedUrl, String healthStatus, boolean enabled) {}
