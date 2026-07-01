package com.newsrss.dto.parser;

/**
 * 订阅源绑定解析模板请求。
 *
 * @param parserTemplateId 解析模板主键，为空时解绑模板并使用默认解析逻辑
 */
public record BindParserTemplateRequest(Long parserTemplateId) {
}
