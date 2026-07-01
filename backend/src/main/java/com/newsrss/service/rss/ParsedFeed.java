package com.newsrss.service.rss;

import com.newsrss.service.parser.TemplateNormalizedArticle;
import java.util.List;

/**
 * 解析后的 RSS Feed 数据，包含订阅源元数据和标准化文章条目。
 *
 * @param title 订阅源标题
 * @param siteUrl 站点地址
 * @param description 订阅源说明
 * @param language 语言代码
 * @param items 标准化文章条目
 * @param templateArticles 模板解析详情，未使用模板时为空列表
 */
public record ParsedFeed(
        String title,
        String siteUrl,
        String description,
        String language,
        List<ParsedArticleItem> items,
        List<TemplateNormalizedArticle> templateArticles) {

    /**
     * 创建默认解析结果。
     *
     * @param title 订阅源标题
     * @param siteUrl 站点地址
     * @param description 订阅源说明
     * @param language 语言代码
     * @param items 标准化文章条目
     * @return 默认解析结果
     */
    public static ParsedFeed defaultParsed(
            String title,
            String siteUrl,
            String description,
            String language,
            List<ParsedArticleItem> items) {
        return new ParsedFeed(title, siteUrl, description, language, items, List.of());
    }

    /**
     * 创建模板解析结果。
     *
     * @param title 订阅源标题
     * @param siteUrl 站点地址
     * @param description 订阅源说明
     * @param language 语言代码
     * @param templateArticles 模板解析详情
     * @return 模板解析结果
     */
    public static ParsedFeed templateParsed(
            String title,
            String siteUrl,
            String description,
            String language,
            List<TemplateNormalizedArticle> templateArticles) {
        List<ParsedArticleItem> items = templateArticles.stream()
                .map(TemplateNormalizedArticle::item)
                .toList();
        return new ParsedFeed(title, siteUrl, description, language, items, templateArticles);
    }
}
