package com.newsrss.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * DeepSeek 模型调用配置。
 *
 * @param enabled 是否启用 DeepSeek 生成
 * @param apiKey DeepSeek API Key
 * @param baseUrl DeepSeek 接口地址
 * @param model DeepSeek 模型名称
 * @param timeoutSeconds 请求超时秒数
 */
@ConfigurationProperties(prefix = "newsrss.ai.deepseek")
public record DeepSeekProperties(
        boolean enabled,
        String apiKey,
        String baseUrl,
        String model,
        int timeoutSeconds) {

    /**
     * 判断配置是否具备真实调用条件。
     *
     * @return 可以调用时返回 true
     */
    public boolean callable() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    /**
     * 获取可用接口地址。
     *
     * @return 接口地址
     */
    public String resolvedBaseUrl() {
        return baseUrl == null || baseUrl.isBlank() ? "https://api.deepseek.com" : baseUrl.strip();
    }

    /**
     * 获取可用模型名称。
     *
     * @return 模型名称
     */
    public String resolvedModel() {
        return model == null || model.isBlank() ? "deepseek-chat" : model.strip();
    }

    /**
     * 获取可用超时时间。
     *
     * @return 超时秒数
     */
    public int resolvedTimeoutSeconds() {
        return timeoutSeconds <= 0 ? 30 : timeoutSeconds;
    }
}
