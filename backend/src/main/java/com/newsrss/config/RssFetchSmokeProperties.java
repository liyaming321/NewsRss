package com.newsrss.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RSS 抓取冒烟验证配置，用于命令行触发一组订阅源抓取。
 */
@ConfigurationProperties(prefix = "newsrss.fetch.smoke")
public class RssFetchSmokeProperties {

    private boolean enabled;

    private List<String> urls = new ArrayList<>();

    /**
     * 判断是否启用抓取冒烟验证。
     *
     * @return 启用时返回 true
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置是否启用抓取冒烟验证。
     *
     * @param enabled 是否启用
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 获取需要抓取的 RSS URL 列表。
     *
     * @return RSS URL 列表
     */
    public List<String> getUrls() {
        return urls;
    }

    /**
     * 设置需要抓取的 RSS URL 列表。
     *
     * @param urls RSS URL 列表
     */
    public void setUrls(List<String> urls) {
        this.urls = urls;
    }
}
