package com.newsrss.service.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.newsrss.domain.entity.RssParserTemplate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 解析模板配置映射器，负责在数据库 JSON 和运行期配置之间转换。
 */
@Component
@Profile("db")
public class ParserTemplateConfigMapper {

    private final ObjectMapper objectMapper;

    public ParserTemplateConfigMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 将模板实体转换成运行期配置。
     *
     * @param template 模板实体
     * @return 运行期配置
     */
    public ParserTemplateConfig fromEntity(RssParserTemplate template) {
        return fromJson(
                template.getTemplateCode(),
                template.getTemplateName(),
                template.getFieldMapping(),
                template.getCustomFieldMapping(),
                template.getContentSelectors(),
                template.getCoverSelectors(),
                template.getTimeFormats(),
                template.getCleanupRules(),
                template.isEnabled());
    }

    /**
     * 将 JSON 配置转换成运行期配置。
     *
     * @param templateCode 模板编码
     * @param templateName 模板名称
     * @param fieldMapping 字段映射
     * @param customFieldMapping 自定义字段映射
     * @param contentSelectors 正文候选字段
     * @param coverSelectors 封面候选字段
     * @param timeFormats 自定义时间格式
     * @param cleanupRules 清洗规则
     * @param enabled 是否启用
     * @return 运行期配置
     */
    public ParserTemplateConfig fromJson(
            String templateCode,
            String templateName,
            JsonNode fieldMapping,
            JsonNode customFieldMapping,
            JsonNode contentSelectors,
            JsonNode coverSelectors,
            JsonNode timeFormats,
            JsonNode cleanupRules,
            boolean enabled) {
        return new ParserTemplateConfig(
                normalizeBlank(templateCode) == null ? "preview-template" : templateCode.strip(),
                normalizeBlank(templateName) == null ? "预览模板" : templateName.strip(),
                parseFieldMapping(fieldMapping),
                parseFieldMapping(customFieldMapping),
                parseStringArray(contentSelectors),
                parseStringArray(coverSelectors),
                parseStringArray(timeFormats),
                parseCleanupRules(cleanupRules),
                enabled);
    }

    /**
     * 创建空对象 JSON。
     *
     * @return 空对象 JSON
     */
    public ObjectNode emptyObject() {
        return objectMapper.createObjectNode();
    }

    /**
     * 创建空数组 JSON。
     *
     * @return 空数组 JSON
     */
    public ArrayNode emptyArray() {
        return objectMapper.createArrayNode();
    }

    /**
     * 解析标准字段到候选路径列表的映射。
     *
     * @param fieldMapping 字段映射 JSON
     * @return 标准字段映射
     */
    private Map<String, List<String>> parseFieldMapping(JsonNode fieldMapping) {
        Map<String, List<String>> mapping = new LinkedHashMap<>();
        if (fieldMapping == null || !fieldMapping.isObject()) {
            return mapping;
        }
        Iterator<Map.Entry<String, JsonNode>> fields = fieldMapping.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            List<String> paths = parseStringArrayOrSingle(field.getValue());
            if (!paths.isEmpty()) {
                mapping.put(field.getKey(), paths);
            }
        }
        return mapping;
    }

    /**
     * 解析单个字符串或字符串数组。
     *
     * @param node JSON 节点
     * @return 字符串列表
     */
    private List<String> parseStringArrayOrSingle(JsonNode node) {
        if (node == null || node.isNull()) {
            return List.of();
        }
        if (node.isTextual()) {
            return normalizeToList(node.asText());
        }
        if (node.isArray()) {
            return parseStringArray(node);
        }
        return List.of();
    }

    /**
     * 解析字符串数组节点。
     *
     * @param node JSON 节点
     * @return 字符串列表
     */
    private List<String> parseStringArray(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        node.forEach(item -> {
            if (item.isTextual()) {
                String value = normalizeBlank(item.asText());
                if (value != null) {
                    values.add(value);
                }
            }
        });
        return List.copyOf(values);
    }

    /**
     * 将非空字符串转换为单元素列表。
     *
     * @param value 原始字符串
     * @return 字符串列表
     */
    private List<String> normalizeToList(String value) {
        String normalizedValue = normalizeBlank(value);
        return normalizedValue == null ? List.of() : List.of(normalizedValue);
    }

    /**
     * 解析正文清洗规则。
     *
     * @param cleanupRules 清洗规则 JSON
     * @return 清洗规则配置
     */
    private ParserTemplateConfig.CleanupRules parseCleanupRules(JsonNode cleanupRules) {
        if (cleanupRules == null || !cleanupRules.isObject()) {
            return new ParserTemplateConfig.CleanupRules(List.of(), List.of(), List.of());
        }
        return new ParserTemplateConfig.CleanupRules(
                parseStringArray(cleanupRules.get("removeSelectors")),
                parseStringArray(cleanupRules.get("unwrapSelectors")),
                parseStringArray(cleanupRules.get("removeAttributes")));
    }

    /**
     * 将空白字符串统一归一为空值。
     *
     * @param value 原始字符串
     * @return 去除首尾空白后的字符串，空白时返回 null
     */
    private String normalizeBlank(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
