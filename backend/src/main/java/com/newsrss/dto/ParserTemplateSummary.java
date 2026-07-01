package com.newsrss.dto;

/**
 * 解析模板摘要 DTO，用于列表和模板绑定场景。
 *
 * @param id 解析模板主键
 * @param templateCode 模板编码
 * @param templateName 模板名称
 * @param enabled 是否启用
 */
public record ParserTemplateSummary(Long id, String templateCode, String templateName, boolean enabled) {}
