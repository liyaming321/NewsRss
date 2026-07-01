package com.newsrss.service.rss;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * RSS 正文清洗器，用于保留安全 HTML 并移除脚本等风险内容。
 */
@Component
@Profile("db")
public class RssContentCleaner {

    private final Safelist safelist = Safelist.relaxed()
            .addTags("figure", "figcaption")
            .addAttributes("img", "src", "alt", "title", "width", "height")
            .addAttributes("a", "href", "title", "target", "rel")
            .addProtocols("img", "src", "http", "https")
            .addProtocols("a", "href", "http", "https");

    /**
     * 清洗 HTML 正文。
     *
     * @param html 原始 HTML
     * @return 清洗后的安全 HTML
     */
    public String clean(String html) {
        if (html == null || html.isBlank()) {
            return null;
        }
        return Jsoup.clean(html, safelist);
    }

    /**
     * 从 HTML 中提取纯文本。
     *
     * @param html HTML 内容
     * @return 纯文本内容
     */
    public String toPlainText(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        return Jsoup.parse(html).text();
    }

    /**
     * 从 HTML 中提取第一张图片地址。
     *
     * @param html HTML 内容
     * @return 图片地址，未命中时返回 null
     */
    public String firstImageUrl(String html) {
        if (html == null || html.isBlank()) {
            return null;
        }
        return Jsoup.parse(html).select("img[src]").stream()
                .map(element -> element.attr("abs:src").isBlank() ? element.attr("src") : element.attr("abs:src"))
                .filter(src -> src != null && !src.isBlank())
                .findFirst()
                .orElse(null);
    }
}
