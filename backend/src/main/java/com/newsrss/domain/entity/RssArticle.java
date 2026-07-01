package com.newsrss.domain.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * RSS 文章实体，保存标准化后的文章内容、来源信息、原始数据和去重指纹。
 */
@Entity
@Table(name = "rss_article")
public class RssArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "feed_id", nullable = false, foreignKey = @ForeignKey(name = "fk_rss_article_feed"))
    private RssFeed feed;

    @Column(name = "guid", length = 1024)
    private String guid;

    @Column(name = "article_url", nullable = false, length = 2048)
    private String articleUrl;

    @Column(name = "canonical_url", length = 2048)
    private String canonicalUrl;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "summary", columnDefinition = "text")
    private String summary;

    @Column(name = "content_html", columnDefinition = "text")
    private String contentHtml;

    @Column(name = "author", length = 255)
    private String author;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "fetched_at", nullable = false)
    private OffsetDateTime fetchedAt;

    @Column(name = "cover_image_url", length = 2048)
    private String coverImageUrl;

    @Column(name = "fingerprint", nullable = false, length = 128)
    private String fingerprint;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", nullable = false, columnDefinition = "jsonb")
    private JsonNode rawPayload;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parse_trace", nullable = false, columnDefinition = "jsonb")
    private JsonNode parseTrace;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "custom_fields", nullable = false, columnDefinition = "jsonb")
    private JsonNode customFields;

    @Column(name = "reading_minutes")
    private Integer readingMinutes;

    @Column(name = "word_count")
    private Integer wordCount;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * 供 JPA 创建文章实体时使用。
     */
    protected RssArticle() {
    }

    /**
     * 创建 RSS 文章实体。
     *
     * @param feed 所属订阅源
     * @param guid RSS 条目 GUID
     * @param articleUrl 文章链接
     * @param title 文章标题
     * @param summary 文章摘要
     * @param contentHtml 清洗后的正文 HTML
     * @param author 作者
     * @param publishedAt 发布时间
     * @param coverImageUrl 封面图地址
     * @param fingerprint 去重指纹
     * @param rawPayload 原始条目数据
     * @param parseTrace 解析命中轨迹
     * @param customFields 自定义字段
     * @param readingMinutes 预计阅读分钟数
     * @param wordCount 文章字数
     * @param now 当前业务时间
     * @return 可持久化的文章实体
     */
    public static RssArticle create(
            RssFeed feed,
            String guid,
            String articleUrl,
            String title,
            String summary,
            String contentHtml,
            String author,
            OffsetDateTime publishedAt,
            String coverImageUrl,
            String fingerprint,
            JsonNode rawPayload,
            JsonNode parseTrace,
            JsonNode customFields,
            Integer readingMinutes,
            Integer wordCount,
            OffsetDateTime now) {
        RssArticle article = new RssArticle();
        article.feed = feed;
        article.guid = guid;
        article.articleUrl = articleUrl;
        article.canonicalUrl = articleUrl;
        article.title = title;
        article.summary = summary;
        article.contentHtml = contentHtml;
        article.author = author;
        article.publishedAt = publishedAt;
        article.fetchedAt = now;
        article.coverImageUrl = coverImageUrl;
        article.fingerprint = fingerprint;
        article.rawPayload = rawPayload;
        article.parseTrace = parseTrace;
        article.customFields = customFields;
        article.readingMinutes = readingMinutes;
        article.wordCount = wordCount;
        article.createdAt = now;
        article.updatedAt = now;
        return article;
    }

    /**
     * 获取文章主键。
     *
     * @return 文章主键
     */
    public Long getId() {
        return id;
    }

    /**
     * 获取所属订阅源。
     *
     * @return 所属订阅源
     */
    public RssFeed getFeed() {
        return feed;
    }

    /**
     * 获取文章 GUID。
     *
     * @return 文章 GUID
     */
    public String getGuid() {
        return guid;
    }

    /**
     * 获取文章链接。
     *
     * @return 文章链接
     */
    public String getArticleUrl() {
        return articleUrl;
    }

    /**
     * 获取规范化链接。
     *
     * @return 规范化链接
     */
    public String getCanonicalUrl() {
        return canonicalUrl;
    }

    /**
     * 获取文章标题。
     *
     * @return 文章标题
     */
    public String getTitle() {
        return title;
    }

    /**
     * 获取文章摘要。
     *
     * @return 文章摘要
     */
    public String getSummary() {
        return summary;
    }

    /**
     * 获取文章正文 HTML。
     *
     * @return 文章正文 HTML
     */
    public String getContentHtml() {
        return contentHtml;
    }

    /**
     * 获取作者。
     *
     * @return 作者
     */
    public String getAuthor() {
        return author;
    }

    /**
     * 获取发布时间。
     *
     * @return 发布时间
     */
    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }

    /**
     * 获取抓取入库时间。
     *
     * @return 抓取入库时间
     */
    public OffsetDateTime getFetchedAt() {
        return fetchedAt;
    }

    /**
     * 获取封面图地址。
     *
     * @return 封面图地址
     */
    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    /**
     * 获取去重指纹。
     *
     * @return 去重指纹
     */
    public String getFingerprint() {
        return fingerprint;
    }

    /**
     * 获取原始条目数据。
     *
     * @return 原始条目数据
     */
    public JsonNode getRawPayload() {
        return rawPayload;
    }

    /**
     * 获取解析轨迹。
     *
     * @return 解析轨迹
     */
    public JsonNode getParseTrace() {
        return parseTrace;
    }

    /**
     * 获取自定义字段。
     *
     * @return 自定义字段
     */
    public JsonNode getCustomFields() {
        return customFields;
    }

    /**
     * 获取预计阅读分钟数。
     *
     * @return 预计阅读分钟数
     */
    public Integer getReadingMinutes() {
        return readingMinutes;
    }

    /**
     * 获取文章字数。
     *
     * @return 文章字数
     */
    public Integer getWordCount() {
        return wordCount;
    }

    /**
     * 获取创建时间。
     *
     * @return 创建时间
     */
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 获取更新时间。
     *
     * @return 更新时间
     */
    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
