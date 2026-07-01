package com.newsrss.dto.parser;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.OffsetDateTime;

/**
 * 解析模板响应。
 *
 * @param id 模板主键
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
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record ParserTemplateResponse(
        Long id,
        String templateCode,
        String templateName,
        String description,
        JsonNode fieldMapping,
        JsonNode customFieldMapping,
        JsonNode contentSelectors,
        JsonNode coverSelectors,
        JsonNode timeFormats,
        JsonNode cleanupRules,
        boolean enabled,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
