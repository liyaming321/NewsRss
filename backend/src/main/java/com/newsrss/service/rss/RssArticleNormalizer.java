package com.newsrss.service.rss;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndEnclosure;
import com.rometools.rome.feed.synd.SyndPerson;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Date;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * RSS 文章标准化器，将 Rome 条目转换为系统内部标准文章结构。
 */
@Component
@Profile("db")
public class RssArticleNormalizer {

    private final ObjectMapper objectMapper;
    private final RssContentCleaner contentCleaner;
    private final RssFingerprintGenerator fingerprintGenerator;

    public RssArticleNormalizer(
            ObjectMapper objectMapper,
            RssContentCleaner contentCleaner,
            RssFingerprintGenerator fingerprintGenerator) {
        this.objectMapper = objectMapper;
        this.contentCleaner = contentCleaner;
        this.fingerprintGenerator = fingerprintGenerator;
    }

    /**
     * 将 Rome 条目标准化为文章条目。
     *
     * @param feedUrl 订阅地址
     * @param entry Rome 条目
     * @return 标准化文章条目
     */
    public ParsedArticleItem normalize(String feedUrl, SyndEntry entry) {
        String articleUrl = requireArticleUrl(entry);
        String title = requireTitle(entry);
        String rawContent = resolveRawContent(entry);
        String contentHtml = contentCleaner.clean(rawContent);
        String summary = normalizeBlank(entry.getDescription() == null ? null : entry.getDescription().getValue());
        String plainText = contentCleaner.toPlainText(contentHtml != null ? contentHtml : summary);
        OffsetDateTime publishedAt = resolvePublishedAt(entry);
        String guid = normalizeBlank(entry.getUri());
        String author = resolveAuthor(entry);
        String coverImageUrl = resolveCoverImageUrl(entry, contentHtml);
        String publishedAtText = publishedAt == null ? null : publishedAt.toString();
        String fingerprint = fingerprintGenerator.generate(feedUrl, guid, articleUrl, title, publishedAtText);
        JsonNode rawPayload = buildRawPayload(entry);
        JsonNode parseTrace = buildParseTrace(entry, contentHtml, coverImageUrl);
        int wordCount = countWords(plainText);
        int readingMinutes = Math.max(1, (int) Math.ceil(wordCount / 400.0));

        return new ParsedArticleItem(
                guid,
                articleUrl,
                title,
                summary,
                contentHtml,
                author,
                publishedAt,
                coverImageUrl,
                fingerprint,
                rawPayload,
                parseTrace,
                objectMapper.createObjectNode(),
                readingMinutes,
                wordCount);
    }

    /**
     * 获取文章链接，缺失时中断当前条目处理。
     *
     * @param entry Rome 条目
     * @return 文章链接
     */
    private String requireArticleUrl(SyndEntry entry) {
        String articleUrl = normalizeBlank(entry.getLink());
        if (articleUrl == null) {
            throw new RssFetchException("RSS 条目缺少文章链接", null);
        }
        return articleUrl;
    }

    /**
     * 获取文章标题，缺失时使用可读默认标题。
     *
     * @param entry Rome 条目
     * @return 文章标题
     */
    private String requireTitle(SyndEntry entry) {
        String title = normalizeBlank(entry.getTitle());
        return title == null ? "未命名文章" : title;
    }

    /**
     * 按内容优先、摘要兜底的顺序获取原始正文。
     *
     * @param entry Rome 条目
     * @return 原始正文
     */
    private String resolveRawContent(SyndEntry entry) {
        Optional<String> firstContent = entry.getContents().stream()
                .map(SyndContent::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst();
        if (firstContent.isPresent()) {
            return firstContent.get();
        }
        return entry.getDescription() == null ? null : entry.getDescription().getValue();
    }

    /**
     * 解析发布时间，发布时间缺失时使用更新时间。
     *
     * @param entry Rome 条目
     * @return UTC 发布时间
     */
    private OffsetDateTime resolvePublishedAt(SyndEntry entry) {
        Date publishedDate = entry.getPublishedDate();
        if (publishedDate == null) {
            publishedDate = entry.getUpdatedDate();
        }
        if (publishedDate == null) {
            return null;
        }
        return OffsetDateTime.ofInstant(publishedDate.toInstant(), ZoneOffset.UTC);
    }

    /**
     * 解析作者，优先使用单值作者字段，再从作者列表兜底。
     *
     * @param entry Rome 条目
     * @return 作者名称
     */
    private String resolveAuthor(SyndEntry entry) {
        String author = normalizeBlank(entry.getAuthor());
        if (author != null) {
            return author;
        }
        return entry.getAuthors().stream()
                .map(SyndPerson::getName)
                .filter(name -> name != null && !name.isBlank())
                .findFirst()
                .orElse(null);
    }

    /**
     * 解析封面图，优先使用图片附件，再从正文第一张图片兜底。
     *
     * @param entry Rome 条目
     * @param contentHtml 清洗后的正文 HTML
     * @return 封面图地址
     */
    private String resolveCoverImageUrl(SyndEntry entry, String contentHtml) {
        Optional<String> imageEnclosure = entry.getEnclosures().stream()
                .filter(enclosure -> isImageEnclosure(enclosure))
                .map(SyndEnclosure::getUrl)
                .filter(url -> url != null && !url.isBlank())
                .findFirst();
        return imageEnclosure.orElseGet(() -> contentCleaner.firstImageUrl(contentHtml));
    }

    /**
     * 判断附件是否为图片类型。
     *
     * @param enclosure RSS 附件
     * @return 图片附件返回 true
     */
    private boolean isImageEnclosure(SyndEnclosure enclosure) {
        String type = enclosure.getType();
        return type != null && type.toLowerCase().startsWith("image/");
    }

    /**
     * 构建原始条目 JSON，用于后续排查和重新解析。
     *
     * @param entry Rome 条目
     * @return 原始条目 JSON
     */
    private JsonNode buildRawPayload(SyndEntry entry) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("uri", entry.getUri());
        payload.put("title", entry.getTitle());
        payload.put("link", entry.getLink());
        payload.put("author", entry.getAuthor());
        payload.put("publishedDate", entry.getPublishedDate() == null ? null : entry.getPublishedDate().toInstant().toString());
        payload.put("updatedDate", entry.getUpdatedDate() == null ? null : entry.getUpdatedDate().toInstant().toString());
        payload.put("description", entry.getDescription() == null ? null : entry.getDescription().getValue());
        payload.set("contents", objectMapper.valueToTree(entry.getContents().stream()
                .map(SyndContent::getValue)
                .toList()));
        payload.set("enclosures", objectMapper.valueToTree(entry.getEnclosures().stream()
                .map(enclosure -> {
                    HashMap<String, String> value = new HashMap<>();
                    value.put("type", enclosure.getType());
                    value.put("url", enclosure.getUrl());
                    return value;
                })
                .toList()));
        return payload;
    }

    /**
     * 构建解析轨迹 JSON，记录默认解析器命中的字段路径。
     *
     * @param entry Rome 条目
     * @param contentHtml 清洗后的正文 HTML
     * @param coverImageUrl 封面图地址
     * @return 解析轨迹 JSON
     */
    private JsonNode buildParseTrace(SyndEntry entry, String contentHtml, String coverImageUrl) {
        ObjectNode trace = objectMapper.createObjectNode();
        trace.put("parser", "default-rome");
        trace.put("titlePath", entry.getTitle() == null ? "fallback:untitled" : "entry.title");
        trace.put("urlPath", "entry.link");
        trace.put("contentPath", entry.getContents().isEmpty() ? "entry.description" : "entry.contents[0]");
        trace.put("publishedAtPath", entry.getPublishedDate() == null ? "entry.updatedDate" : "entry.publishedDate");
        trace.put("coverImagePath", coverImageUrl == null ? null : "entry.enclosures|content.img");
        trace.put("contentCleaned", contentHtml != null);
        return trace;
    }

    /**
     * 估算正文词数，中文按非空白字符计数，其他语言按空白切词。
     *
     * @param text 正文纯文本
     * @return 估算词数
     */
    private int countWords(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        String compactText = text.strip();
        if (compactText.chars().anyMatch(character -> Character.UnicodeScript.of(character) == Character.UnicodeScript.HAN)) {
            return (int) compactText.chars()
                    .filter(character -> !Character.isWhitespace(character))
                    .count();
        }
        return compactText.split("\\s+").length;
    }

    /**
     * 将空白字符串统一归一为空值。
     *
     * @param value 原始字符串
     * @return 去除首尾空白后的字符串，空白时返回 null
     */
    private String normalizeBlank(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }
}
