package com.newsrss.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RSS 定时抓取配置。
 */
@ConfigurationProperties(prefix = "newsrss.fetch.scheduler")
public class RssFetchSchedulerProperties {

    private boolean enabled = true;

    private long fixedDelayMs = 60_000L;

    private long initialDelayMs = 15_000L;

    private int batchSize = 5;

    /**
     * 判断是否启用定时抓取。
     *
     * @return 启用时返回 true
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置是否启用定时抓取。
     *
     * @param enabled 是否启用
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 获取固定延迟毫秒数。
     *
     * @return 固定延迟毫秒数
     */
    public long getFixedDelayMs() {
        return fixedDelayMs;
    }

    /**
     * 设置固定延迟毫秒数。
     *
     * @param fixedDelayMs 固定延迟毫秒数
     */
    public void setFixedDelayMs(long fixedDelayMs) {
        this.fixedDelayMs = fixedDelayMs;
    }

    /**
     * 获取初始延迟毫秒数。
     *
     * @return 初始延迟毫秒数
     */
    public long getInitialDelayMs() {
        return initialDelayMs;
    }

    /**
     * 设置初始延迟毫秒数。
     *
     * @param initialDelayMs 初始延迟毫秒数
     */
    public void setInitialDelayMs(long initialDelayMs) {
        this.initialDelayMs = initialDelayMs;
    }

    /**
     * 获取每批抓取数量。
     *
     * @return 每批抓取数量
     */
    public int getBatchSize() {
        return batchSize;
    }

    /**
     * 设置每批抓取数量。
     *
     * @param batchSize 每批抓取数量
     */
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
}
