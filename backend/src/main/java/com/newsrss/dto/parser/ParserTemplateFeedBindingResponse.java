package com.newsrss.dto.parser;

/**
 * 订阅源模板绑定响应。
 *
 * @param feedId 订阅源主键
 * @param feedUrl RSS 或 Atom 订阅地址
 * @param parserTemplateId 当前绑定模板主键
 * @param templateCode 当前绑定模板编码
 */
public record ParserTemplateFeedBindingResponse(
        Long feedId,
        String feedUrl,
        Long parserTemplateId,
        String templateCode) {
}
