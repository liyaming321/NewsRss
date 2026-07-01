package com.newsrss.service.parser;

/**
 * 模板字段命中结果。
 *
 * @param fieldName 标准字段名称
 * @param matched 是否命中
 * @param path 命中的字段路径
 * @param value 命中的文本值
 * @param fallback 是否使用默认解析兜底
 * @param message 命中说明
 */
public record TemplateFieldHit(
        String fieldName,
        boolean matched,
        String path,
        String value,
        boolean fallback,
        String message) {
}
