package com.newsrss.dto.parser;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 解析模板保存请求。
 *
 * @param templateCode 模板编码
 * @param templateName 模板名称
 * @param description 模板说明
 * @param fieldMapping 标准字段映射配置
 * @param customFieldMapping 自定义字段映射配置
 * @param contentSelectors 正文候选字段列表
 * @param coverSelectors 封面候选字段列表
 * @param timeFormats 自定义时间格式列表
 * @param cleanupRules 正文清洗规则
 * @param enabled 是否启用
 */
public record ParserTemplateRequest(
        @NotBlank(message = "模板编码不能为空")
        @Size(max = 80, message = "模板编码不能超过 80 个字符")
        String templateCode,
        @NotBlank(message = "模板名称不能为空")
        @Size(max = 120, message = "模板名称不能超过 120 个字符")
        String templateName,
        String description,
        JsonNode fieldMapping,
        JsonNode customFieldMapping,
        JsonNode contentSelectors,
        JsonNode coverSelectors,
        JsonNode timeFormats,
        JsonNode cleanupRules,
        Boolean enabled) {
}
