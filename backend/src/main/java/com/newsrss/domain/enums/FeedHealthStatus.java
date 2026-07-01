package com.newsrss.domain.enums;

/**
 * 订阅源健康状态，用于表达抓取服务对订阅源可用性的判断。
 */
public enum FeedHealthStatus {
    UNKNOWN,
    HEALTHY,
    WARNING,
    ERROR
}
