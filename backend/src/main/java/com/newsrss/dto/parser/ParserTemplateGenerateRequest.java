package com.newsrss.dto.parser;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 从订阅源生成解析模板请求。
 *
 * @param feedId 已保存订阅源主键
 * @param feedUrl 临时 RSS 或 Atom 地址
 * @param limit 样本条数
 * @param preferAi 是否优先使用 AI 生成
 */
public record ParserTemplateGenerateRequest(
        Long feedId,
        String feedUrl,
        @Min(value = 1, message = "样本条数必须大于 0")
        @Max(value = 10, message = "样本条数不能超过 10")
        Integer limit,
        Boolean preferAi) {
}
