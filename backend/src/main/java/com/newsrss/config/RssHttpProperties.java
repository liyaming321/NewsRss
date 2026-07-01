package com.newsrss.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RSS 远端 HTTP 访问配置，控制抓取和模板解析共用的网络参数。
 */
@ConfigurationProperties(prefix = "newsrss.rss.http")
public class RssHttpProperties {

    private static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = 10;
    private static final int DEFAULT_READ_TIMEOUT_SECONDS = 60;
    private static final String DEFAULT_USER_AGENT = "NewsRss/0.0.1 (+https://local.newsrss)";

    private int connectTimeoutSeconds = DEFAULT_CONNECT_TIMEOUT_SECONDS;

    private int readTimeoutSeconds = DEFAULT_READ_TIMEOUT_SECONDS;

    private String userAgent = DEFAULT_USER_AGENT;

    /**
     * 获取连接超时秒数。
     *
     * @return 连接超时秒数
     */
    public int getConnectTimeoutSeconds() {
        return connectTimeoutSeconds;
    }

    /**
     * 设置连接超时秒数。
     *
     * @param connectTimeoutSeconds 连接超时秒数
     */
    public void setConnectTimeoutSeconds(int connectTimeoutSeconds) {
        this.connectTimeoutSeconds = connectTimeoutSeconds;
    }

    /**
     * 获取读取超时秒数。
     *
     * @return 读取超时秒数
     */
    public int getReadTimeoutSeconds() {
        return readTimeoutSeconds;
    }

    /**
     * 设置读取超时秒数。
     *
     * @param readTimeoutSeconds 读取超时秒数
     */
    public void setReadTimeoutSeconds(int readTimeoutSeconds) {
        this.readTimeoutSeconds = readTimeoutSeconds;
    }

    /**
     * 获取请求 User-Agent。
     *
     * @return 请求 User-Agent
     */
    public String getUserAgent() {
        return userAgent;
    }

    /**
     * 设置请求 User-Agent。
     *
     * @param userAgent 请求 User-Agent
     */
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    /**
     * 获取可用连接超时时长。
     *
     * @return 连接超时时长
     */
    public Duration resolvedConnectTimeout() {
        return Duration.ofSeconds(connectTimeoutSeconds <= 0 ? DEFAULT_CONNECT_TIMEOUT_SECONDS : connectTimeoutSeconds);
    }

    /**
     * 获取可用读取超时时长。
     *
     * @return 读取超时时长
     */
    public Duration resolvedReadTimeout() {
        return Duration.ofSeconds(readTimeoutSeconds <= 0 ? DEFAULT_READ_TIMEOUT_SECONDS : readTimeoutSeconds);
    }

    /**
     * 获取可用请求 User-Agent。
     *
     * @return 请求 User-Agent
     */
    public String resolvedUserAgent() {
        return userAgent == null || userAgent.isBlank() ? DEFAULT_USER_AGENT : userAgent.strip();
    }
}
