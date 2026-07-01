package com.newsrss.domain.entity;

import com.newsrss.domain.enums.FetchLogStatus;
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
 * RSS 抓取日志实体，记录一次抓取的请求地址、状态、数量统计和错误信息。
 */
@Entity
@Table(name = "rss_feed_fetch_log")
public class RssFeedFetchLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "feed_id", nullable = false, foreignKey = @ForeignKey(name = "fk_rss_feed_fetch_log_feed"))
    private RssFeed feed;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private FetchLogStatus status = FetchLogStatus.RUNNING;

    @Column(name = "request_url", nullable = false, length = 2048)
    private String requestUrl;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "fetched_count", nullable = false)
    private Integer fetchedCount = 0;

    @Column(name = "new_count", nullable = false)
    private Integer newCount = 0;

    @Column(name = "duplicate_count", nullable = false)
    private Integer duplicateCount = 0;

    @Column(name = "failed_count", nullable = false)
    private Integer failedCount = 0;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "error_stack", columnDefinition = "text")
    private String errorStack;

    @Column(name = "raw_response_sample", columnDefinition = "text")
    private String rawResponseSample;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    /**
     * 供 JPA 创建抓取日志实体时使用。
     */
    protected RssFeedFetchLog() {
    }

    /**
     * 创建抓取开始日志。
     *
     * @param feed 订阅源
     * @param requestUrl 本次请求地址
     * @param startedAt 抓取开始时间
     * @return 可持久化的抓取日志
     */
    public static RssFeedFetchLog start(RssFeed feed, String requestUrl, OffsetDateTime startedAt) {
        RssFeedFetchLog fetchLog = new RssFeedFetchLog();
        fetchLog.feed = feed;
        fetchLog.requestUrl = requestUrl;
        fetchLog.startedAt = startedAt;
        fetchLog.status = FetchLogStatus.RUNNING;
        fetchLog.fetchedCount = 0;
        fetchLog.newCount = 0;
        fetchLog.duplicateCount = 0;
        fetchLog.failedCount = 0;
        fetchLog.createdAt = startedAt;
        return fetchLog;
    }

    /**
     * 标记抓取成功。
     *
     * @param fetchedCount 抓取条目数
     * @param newCount 新增文章数
     * @param duplicateCount 重复文章数
     * @param failedCount 失败条目数
     * @param finishedAt 抓取结束时间
     */
    public void markSucceeded(
            int fetchedCount,
            int newCount,
            int duplicateCount,
            int failedCount,
            OffsetDateTime finishedAt) {
        this.status = failedCount > 0 ? FetchLogStatus.PARTIAL : FetchLogStatus.SUCCESS;
        this.fetchedCount = fetchedCount;
        this.newCount = newCount;
        this.duplicateCount = duplicateCount;
        this.failedCount = failedCount;
        this.finishedAt = finishedAt;
        this.durationMs = java.time.Duration.between(startedAt, finishedAt).toMillis();
    }

    /**
     * 标记抓取失败。
     *
     * @param errorMessage 可读错误信息
     * @param errorStack 异常堆栈摘要
     * @param finishedAt 抓取结束时间
     */
    public void markFailed(String errorMessage, String errorStack, OffsetDateTime finishedAt) {
        this.status = FetchLogStatus.FAILED;
        this.errorMessage = errorMessage;
        this.errorStack = errorStack;
        this.finishedAt = finishedAt;
        this.durationMs = java.time.Duration.between(startedAt, finishedAt).toMillis();
    }

    /**
     * 获取抓取日志主键。
     *
     * @return 抓取日志主键
     */
    public Long getId() {
        return id;
    }

    /**
     * 获取订阅源。
     *
     * @return 订阅源
     */
    public RssFeed getFeed() {
        return feed;
    }

    /**
     * 获取抓取开始时间。
     *
     * @return 抓取开始时间
     */
    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    /**
     * 获取抓取结束时间。
     *
     * @return 抓取结束时间
     */
    public OffsetDateTime getFinishedAt() {
        return finishedAt;
    }

    /**
     * 获取抓取状态。
     *
     * @return 抓取状态
     */
    public FetchLogStatus getStatus() {
        return status;
    }

    /**
     * 获取请求地址。
     *
     * @return 请求地址
     */
    public String getRequestUrl() {
        return requestUrl;
    }

    /**
     * 获取 HTTP 状态码。
     *
     * @return HTTP 状态码
     */
    public Integer getHttpStatus() {
        return httpStatus;
    }

    /**
     * 获取抓取条目数。
     *
     * @return 抓取条目数
     */
    public Integer getFetchedCount() {
        return fetchedCount;
    }

    /**
     * 获取新增文章数。
     *
     * @return 新增文章数
     */
    public Integer getNewCount() {
        return newCount;
    }

    /**
     * 获取重复文章数。
     *
     * @return 重复文章数
     */
    public Integer getDuplicateCount() {
        return duplicateCount;
    }

    /**
     * 获取失败条目数。
     *
     * @return 失败条目数
     */
    public Integer getFailedCount() {
        return failedCount;
    }

    /**
     * 获取抓取耗时毫秒数。
     *
     * @return 抓取耗时毫秒数
     */
    public Long getDurationMs() {
        return durationMs;
    }

    /**
     * 获取错误信息。
     *
     * @return 错误信息
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * 获取错误堆栈。
     *
     * @return 错误堆栈
     */
    public String getErrorStack() {
        return errorStack;
    }

    /**
     * 获取原始响应样本。
     *
     * @return 原始响应样本
     */
    public String getRawResponseSample() {
        return rawResponseSample;
    }

    /**
     * 获取创建时间。
     *
     * @return 创建时间
     */
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
