package com.newsrss.domain.entity;

import com.newsrss.domain.enums.FeedHealthStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/**
 * RSS 订阅源实体，保存订阅地址、抓取频率、健康状态和解析模板绑定关系。
 */
@Entity
@Table(name = "rss_feed")
public class RssFeed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "parser_template_id",
            foreignKey = @ForeignKey(name = "fk_rss_feed_parser_template"))
    private RssParserTemplate parserTemplate;

    @Column(name = "feed_name", nullable = false, length = 160)
    private String feedName;

    @Column(name = "feed_url", nullable = false, length = 1024)
    private String feedUrl;

    @Column(name = "site_url", length = 1024)
    private String siteUrl;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "language", length = 32)
    private String language;

    @Column(name = "category", length = 80)
    private String category;

    @Column(name = "icon_url", length = 1024)
    private String iconUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "health_status", nullable = false, length = 32)
    private FeedHealthStatus healthStatus = FeedHealthStatus.UNKNOWN;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "fetch_interval_minutes", nullable = false)
    private Integer fetchIntervalMinutes = 60;

    @Column(name = "last_fetch_at")
    private OffsetDateTime lastFetchAt;

    @Column(name = "next_fetch_at")
    private OffsetDateTime nextFetchAt;

    @Column(name = "last_success_at")
    private OffsetDateTime lastSuccessAt;

    @Column(name = "last_failure_at")
    private OffsetDateTime lastFailureAt;

    @Column(name = "consecutive_failure_count", nullable = false)
    private Integer consecutiveFailureCount = 0;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * 供 JPA 创建订阅源实体时使用。
     */
    protected RssFeed() {
    }

    /**
     * 创建一个新的 RSS 订阅源。
     *
     * @param feedName 订阅源名称
     * @param feedUrl RSS 或 Atom 订阅地址
     * @param fetchIntervalMinutes 抓取间隔分钟数
     * @param now 当前业务时间
     * @return 可持久化的订阅源实体
     */
    public static RssFeed create(String feedName, String feedUrl, Integer fetchIntervalMinutes, OffsetDateTime now) {
        RssFeed feed = new RssFeed();
        feed.feedName = feedName;
        feed.feedUrl = feedUrl;
        feed.fetchIntervalMinutes = fetchIntervalMinutes == null ? 60 : fetchIntervalMinutes;
        feed.healthStatus = FeedHealthStatus.UNKNOWN;
        feed.enabled = true;
        feed.consecutiveFailureCount = 0;
        feed.createdAt = now;
        feed.updatedAt = now;
        return feed;
    }

    /**
     * 根据远端 Feed 元数据更新订阅源基础信息。
     *
     * @param feedName 远端 Feed 名称
     * @param siteUrl 站点地址
     * @param description 订阅源说明
     * @param language 语言代码
     * @param now 当前业务时间
     */
    public void refreshMetadata(
            String feedName,
            String siteUrl,
            String description,
            String language,
            OffsetDateTime now) {
        if (feedName != null && !feedName.isBlank()) {
            this.feedName = feedName;
        }
        this.siteUrl = siteUrl;
        this.description = description;
        this.language = language;
        this.updatedAt = now;
    }

    /**
     * 更新订阅源人工配置。
     *
     * @param feedName 订阅源名称
     * @param feedUrl RSS 或 Atom 地址
     * @param category 分类
     * @param iconUrl 图标地址
     * @param fetchIntervalMinutes 抓取间隔分钟数
     * @param enabled 是否启用
     * @param now 当前业务时间
     */
    public void updateManualConfig(
            String feedName,
            String feedUrl,
            String category,
            String iconUrl,
            Integer fetchIntervalMinutes,
            boolean enabled,
            OffsetDateTime now) {
        this.feedName = feedName;
        this.feedUrl = feedUrl;
        this.category = category;
        this.iconUrl = iconUrl;
        this.fetchIntervalMinutes = fetchIntervalMinutes;
        this.enabled = enabled;
        this.updatedAt = now;
    }

    /**
     * 标记订阅源抓取开始。
     *
     * @param now 当前业务时间
     */
    public void markFetchStarted(OffsetDateTime now) {
        this.lastFetchAt = now;
        this.updatedAt = now;
    }

    /**
     * 标记订阅源抓取成功，并刷新下一次抓取时间。
     *
     * @param now 当前业务时间
     */
    public void markFetchSucceeded(OffsetDateTime now) {
        this.healthStatus = FeedHealthStatus.HEALTHY;
        this.lastSuccessAt = now;
        this.consecutiveFailureCount = 0;
        this.nextFetchAt = now.plusMinutes(fetchIntervalMinutes);
        this.updatedAt = now;
    }

    /**
     * 标记订阅源抓取失败，并累加连续失败次数。
     *
     * @param now 当前业务时间
     */
    public void markFetchFailed(OffsetDateTime now) {
        this.lastFailureAt = now;
        this.consecutiveFailureCount += 1;
        this.healthStatus = this.consecutiveFailureCount >= 3 ? FeedHealthStatus.ERROR : FeedHealthStatus.WARNING;
        this.nextFetchAt = now.plusMinutes(fetchIntervalMinutes);
        this.updatedAt = now;
    }

    /**
     * 绑定解析模板。
     *
     * @param parserTemplate 解析模板，为空时表示解绑
     * @param now 当前业务时间
     */
    public void bindParserTemplate(RssParserTemplate parserTemplate, OffsetDateTime now) {
        this.parserTemplate = parserTemplate;
        this.updatedAt = now;
    }

    /**
     * 获取订阅源主键。
     *
     * @return 订阅源主键
     */
    public Long getId() {
        return id;
    }

    /**
     * 获取订阅地址。
     *
     * @return RSS 或 Atom 订阅地址
     */
    public String getFeedUrl() {
        return feedUrl;
    }

    /**
     * 获取订阅源名称。
     *
     * @return 订阅源名称
     */
    public String getFeedName() {
        return feedName;
    }

    /**
     * 获取站点地址。
     *
     * @return 站点地址
     */
    public String getSiteUrl() {
        return siteUrl;
    }

    /**
     * 获取订阅源说明。
     *
     * @return 订阅源说明
     */
    public String getDescription() {
        return description;
    }

    /**
     * 获取语言代码。
     *
     * @return 语言代码
     */
    public String getLanguage() {
        return language;
    }

    /**
     * 获取订阅源分类。
     *
     * @return 订阅源分类
     */
    public String getCategory() {
        return category;
    }

    /**
     * 获取图标地址。
     *
     * @return 图标地址
     */
    public String getIconUrl() {
        return iconUrl;
    }

    /**
     * 获取健康状态。
     *
     * @return 健康状态
     */
    public FeedHealthStatus getHealthStatus() {
        return healthStatus;
    }

    /**
     * 判断订阅源是否启用。
     *
     * @return 启用时返回 true
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 获取抓取间隔分钟数。
     *
     * @return 抓取间隔分钟数
     */
    public Integer getFetchIntervalMinutes() {
        return fetchIntervalMinutes;
    }

    /**
     * 获取最近抓取时间。
     *
     * @return 最近抓取时间
     */
    public OffsetDateTime getLastFetchAt() {
        return lastFetchAt;
    }

    /**
     * 获取下次抓取时间。
     *
     * @return 下次抓取时间
     */
    public OffsetDateTime getNextFetchAt() {
        return nextFetchAt;
    }

    /**
     * 获取最近成功时间。
     *
     * @return 最近成功时间
     */
    public OffsetDateTime getLastSuccessAt() {
        return lastSuccessAt;
    }

    /**
     * 获取最近失败时间。
     *
     * @return 最近失败时间
     */
    public OffsetDateTime getLastFailureAt() {
        return lastFailureAt;
    }

    /**
     * 获取连续失败次数。
     *
     * @return 连续失败次数
     */
    public Integer getConsecutiveFailureCount() {
        return consecutiveFailureCount;
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

    /**
     * 获取已绑定解析模板。
     *
     * @return 解析模板，未绑定时为空
     */
    public RssParserTemplate getParserTemplate() {
        return parserTemplate;
    }
}
