package com.newsrss.service.parser;

import java.util.List;
import java.util.Map;

/**
 * 解析模板配置值对象。
 *
 * @param templateCode 模板编码
 * @param templateName 模板名称
 * @param fieldMapping 标准字段映射
 * @param customFieldMapping 自定义字段映射
 * @param contentSelectors 正文候选字段
 * @param coverSelectors 封面候选字段
 * @param timeFormats 自定义时间格式
 * @param cleanupRules 清洗规则
 * @param enabled 是否启用
 */
public record ParserTemplateConfig(
        String templateCode,
        String templateName,
        Map<String, List<String>> fieldMapping,
        Map<String, List<String>> customFieldMapping,
        List<String> contentSelectors,
        List<String> coverSelectors,
        List<String> timeFormats,
        CleanupRules cleanupRules,
        boolean enabled) {

    /**
     * 正文清洗规则。
     *
     * @param removeSelectors 需要移除的 CSS 选择器
     * @param unwrapSelectors 需要去除标签但保留内容的 CSS 选择器
     * @param removeAttributes 需要移除的 HTML 属性
     */
    public record CleanupRules(
            List<String> removeSelectors,
            List<String> unwrapSelectors,
            List<String> removeAttributes) {
    }
}
