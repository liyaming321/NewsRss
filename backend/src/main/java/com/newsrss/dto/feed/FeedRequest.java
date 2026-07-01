package com.newsrss.dto.feed;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 订阅源保存请求。
 *
 * @param feedName 订阅源名称
 * @param feedUrl RSS 或 Atom 地址
 * @param category 分类
 * @param iconUrl 图标地址
 * @param parserTemplateId 解析模板主键
 * @param fetchIntervalMinutes 抓取间隔分钟数
 * @param enabled 是否启用
 */
public record FeedRequest(
        @NotBlank(message = "订阅源名称不能为空")
        @Size(max = 160, message = "订阅源名称不能超过 160 个字符")
        String feedName,
        @NotBlank(message = "RSS 地址不能为空")
        @Size(max = 1024, message = "RSS 地址不能超过 1024 个字符")
        String feedUrl,
        @Size(max = 80, message = "分类不能超过 80 个字符")
        String category,
        @Size(max = 1024, message = "图标地址不能超过 1024 个字符")
        String iconUrl,
        Long parserTemplateId,
        @Min(value = 1, message = "抓取间隔必须大于 0")
        Integer fetchIntervalMinutes,
        Boolean enabled) {
}
