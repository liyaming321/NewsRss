package com.newsrss.service.rss;

/**
 * RSS 抓取异常，用于包装网络、解析和数据标准化过程中的可读错误。
 */
public class RssFetchException extends RuntimeException {

    /**
     * 创建 RSS 抓取异常。
     *
     * @param message 可读错误信息
     * @param cause 原始异常
     */
    public RssFetchException(String message, Throwable cause) {
        super(message, cause);
    }
}
