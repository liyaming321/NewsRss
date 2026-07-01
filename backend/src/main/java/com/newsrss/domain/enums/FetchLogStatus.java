package com.newsrss.domain.enums;

/**
 * RSS 抓取日志状态，用于记录一次抓取任务的最终结果。
 */
public enum FetchLogStatus {
    RUNNING,
    SUCCESS,
    FAILED,
    PARTIAL
}
