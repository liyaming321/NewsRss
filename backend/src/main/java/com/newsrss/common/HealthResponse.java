package com.newsrss.common;

import java.time.Instant;

/**
 * 后端健康检查响应，用于表达服务名称、版本和当前状态。
 *
 * @param applicationName 应用展示名称
 * @param version 应用版本号
 * @param status 服务状态
 * @param checkedAt 检查时间
 */
public record HealthResponse(String applicationName, String version, String status, Instant checkedAt) {}
