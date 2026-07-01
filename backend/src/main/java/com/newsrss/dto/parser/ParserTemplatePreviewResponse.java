package com.newsrss.dto.parser;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * 解析模板预览响应。
 *
 * @param feedUrl RSS 或 Atom 订阅地址
 * @param feedTitle 远端 Feed 标题
 * @param templateCode 模板编码
 * @param previewLimit 预览条数上限
 * @param itemCount 返回条目数
 * @param hitRate 字段命中率
 * @param fieldHitRates 各标准字段命中率
 * @param warnings 预览级异常提示
 * @param items 预览条目列表
 */
public record ParserTemplatePreviewResponse(
        String feedUrl,
        String feedTitle,
        String templateCode,
        int previewLimit,
        int itemCount,
        double hitRate,
        Map<String, Double> fieldHitRates,
        List<String> warnings,
        List<PreviewItem> items) {

    /**
     * 单条文章预览结果。
     *
     * @param index 条目序号
     * @param rawPayload 原始字段快照
     * @param normalized 标准化结果
     * @param fieldHits 字段命中路径
     * @param customFieldHits 自定义字段命中路径
     * @param warnings 条目级异常提示
     */
    public record PreviewItem(
            int index,
            JsonNode rawPayload,
            NormalizedArticle normalized,
            Map<String, FieldHit> fieldHits,
            Map<String, FieldHit> customFieldHits,
            List<String> warnings) {
    }

    /**
     * 标准化文章预览内容。
     *
     * @param guid RSS 条目 GUID
     * @param articleUrl 文章链接
     * @param title 文章标题
     * @param summary 文章摘要
     * @param contentHtml 清洗后的正文 HTML
     * @param customFields 自定义字段
     * @param author 作者
     * @param publishedAt 发布时间
     * @param coverImageUrl 封面图地址
     * @param readingMinutes 预计阅读分钟数
     * @param wordCount 文章字数
     */
    public record NormalizedArticle(
            String guid,
            String articleUrl,
            String title,
            String summary,
            String contentHtml,
            JsonNode customFields,
            String author,
            OffsetDateTime publishedAt,
            String coverImageUrl,
            Integer readingMinutes,
            Integer wordCount) {
    }

    /**
     * 字段命中信息。
     *
     * @param matched 是否命中
     * @param path 命中的原始字段路径
     * @param value 命中的文本值
     * @param fallback 是否使用默认解析兜底
     * @param message 额外说明
     */
    public record FieldHit(
            boolean matched,
            String path,
            String value,
            boolean fallback,
            String message) {
    }
}
