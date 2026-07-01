package com.newsrss.dto.parser;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/**
 * 从订阅源生成解析模板响应。
 *
 * @param feedId 订阅源主键
 * @param feedUrl RSS 或 Atom 地址
 * @param feedTitle 订阅源标题
 * @param generator 生成器名称
 * @param aiUsed 是否使用 AI
 * @param fallbackUsed 是否使用本地兜底
 * @param template 生成的模板配置
 * @param preview 生成模板后的预览结果
 * @param samplePayloads 样本原始字段
 * @param warnings 生成过程提示
 */
public record ParserTemplateGenerateResponse(
        Long feedId,
        String feedUrl,
        String feedTitle,
        String generator,
        boolean aiUsed,
        boolean fallbackUsed,
        ParserTemplateRequest template,
        ParserTemplatePreviewResponse preview,
        List<JsonNode> samplePayloads,
        List<String> warnings) {
}
