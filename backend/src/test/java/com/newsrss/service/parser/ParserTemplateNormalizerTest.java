package com.newsrss.service.parser;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newsrss.service.rss.RssContentCleaner;
import com.newsrss.service.rss.RssFingerprintGenerator;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ParserTemplateNormalizerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ParserTemplateNormalizer normalizer = new ParserTemplateNormalizer(
            objectMapper,
            new RssContentCleaner(),
            new RssFingerprintGenerator());

    /**
     * 验证模板字段映射、候选字段优先级、自定义时间格式和清洗规则可以一起生效。
     */
    @Test
    void shouldNormalizeArticleWithTemplateRules() throws Exception {
        JsonNode rawPayload = objectMapper.readTree("""
                {
                  "uri": "default-guid",
                  "title": "默认标题",
                  "link": "https://example.com/default",
                  "description": {"value": "默认摘要"},
                  "publishedDate": "2026-06-30T08:00:00Z",
                  "foreignMarkup": {
                    "customGuid": {"value": "custom-guid-1"},
                    "headline": {"value": "模板标题"},
                    "articleUrl": {"value": "https://example.com/custom-article"},
                    "lead": {"value": "模板摘要"},
                    "body": {"value": "<article><p>正文内容</p><script>bad()</script><div class=\\"ad\\">广告</div><img src=\\"https://example.com/a.jpg\\" onclick=\\"bad()\\" /></article>"},
                    "cover": {"attributes": {"url": "https://example.com/cover.jpg"}},
                    "publishedText": {"value": "2026-06-30 16:20:00"},
                    "commentsUrl": {"value": "https://example.com/comments"}
                  },
                  "authors": [{"name": "默认作者"}],
                  "enclosures": [{"url": "https://example.com/default-cover.jpg", "type": "image/jpeg"}]
                }
                """);
        ParserTemplateConfig config = new ParserTemplateConfig(
                "custom-news",
                "自定义新闻模板",
                Map.of(
                        "guid", List.of("foreignMarkup.customGuid.value"),
                        "title", List.of("foreignMarkup.headline.value"),
                        "articleUrl", List.of("foreignMarkup.articleUrl.value"),
                        "summary", List.of("foreignMarkup.lead.value"),
                        "publishedAt", List.of("foreignMarkup.publishedText.value")),
                Map.of("commentsUrl", List.of("foreignMarkup.commentsUrl.value")),
                List.of("foreignMarkup.body.value"),
                List.of("foreignMarkup.cover.attributes.url"),
                List.of("yyyy-MM-dd HH:mm:ss"),
                new ParserTemplateConfig.CleanupRules(List.of(".ad"), List.of("article"), List.of("onclick")),
                true);

        TemplateNormalizedArticle result = normalizer.normalize("https://example.com/rss.xml", rawPayload, config);

        assertThat(result.item().guid()).isEqualTo("custom-guid-1");
        assertThat(result.item().articleUrl()).isEqualTo("https://example.com/custom-article");
        assertThat(result.item().title()).isEqualTo("模板标题");
        assertThat(result.item().summary()).isEqualTo("模板摘要");
        assertThat(result.item().publishedAt()).isEqualTo(OffsetDateTime.parse("2026-06-30T16:20:00Z"));
        assertThat(result.item().coverImageUrl()).isEqualTo("https://example.com/cover.jpg");
        assertThat(result.item().contentHtml()).contains("正文内容");
        assertThat(result.item().contentHtml()).doesNotContain("script", "广告", "onclick");
        assertThat(result.fieldHits().get("title").path()).isEqualTo("foreignMarkup.headline.value");
        assertThat(result.fieldHits().get("contentHtml").path()).isEqualTo("foreignMarkup.body.value");
        assertThat(result.customFieldHits().get("commentsUrl").path()).isEqualTo("foreignMarkup.commentsUrl.value");
        assertThat(result.item().customFields().get("commentsUrl").asText()).isEqualTo("https://example.com/comments");
        assertThat(result.warnings()).isEmpty();
    }

    /**
     * 验证模板字段缺失时会回退默认字段，并给出时间解析失败提示。
     */
    @Test
    void shouldReportFallbackAndWarningsWhenFieldsMissing() throws Exception {
        JsonNode rawPayload = objectMapper.readTree("""
                {
                  "title": "默认标题",
                  "link": "https://example.com/default",
                  "description": {"value": "默认摘要"},
                  "publishedDate": "not-a-time",
                  "contents": [{"value": "<p>默认正文</p>"}],
                  "enclosures": []
                }
                """);
        ParserTemplateConfig config = new ParserTemplateConfig(
                "broken-template",
                "缺字段模板",
                Map.of(
                        "title", List.of("foreignMarkup.missingTitle.value"),
                        "articleUrl", List.of("foreignMarkup.missingUrl.value"),
                        "publishedAt", List.of("foreignMarkup.missingPublished.value")),
                Map.of(),
                List.of("foreignMarkup.missingBody.value"),
                List.of("foreignMarkup.missingCover.attributes.url"),
                List.of("yyyy/MM/dd HH:mm:ss"),
                new ParserTemplateConfig.CleanupRules(List.of(), List.of(), List.of()),
                true);

        TemplateNormalizedArticle result = normalizer.normalize("https://example.com/rss.xml", rawPayload, config);

        assertThat(result.item().title()).isEqualTo("默认标题");
        assertThat(result.fieldHits().get("title").fallback()).isTrue();
        assertThat(result.fieldHits().get("title").path()).isEqualTo("title");
        assertThat(result.fieldHits().get("contentHtml").fallback()).isTrue();
        assertThat(result.fieldHits().get("contentHtml").path()).isEqualTo("contents[0].value");
        assertThat(result.item().publishedAt()).isNull();
        assertThat(result.warnings()).contains("publishedAt 时间解析失败：not-a-time");
    }
}
