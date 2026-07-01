package com.newsrss.service.rss;

import com.newsrss.domain.entity.RssArticle;
import com.newsrss.domain.entity.RssFeed;
import com.newsrss.domain.entity.RssFeedFetchLog;
import com.newsrss.common.ResourceNotFoundException;
import com.newsrss.dto.RssFetchResult;
import com.newsrss.repository.RssArticleRepository;
import com.newsrss.repository.RssFeedFetchLogRepository;
import com.newsrss.repository.RssFeedRepository;
import com.newsrss.service.parser.ParserTemplateConfig;
import com.newsrss.service.parser.ParserTemplateConfigMapper;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RSS 抓取服务，负责编排抓取、标准化、去重、入库和日志记录。
 */
@Service
@Profile("db")
public class RssFeedFetchService {

    private static final int DEFAULT_FETCH_INTERVAL_MINUTES = 60;

    private final RssFeedRepository feedRepository;
    private final RssArticleRepository articleRepository;
    private final RssFeedFetchLogRepository fetchLogRepository;
    private final RssFeedParser feedParser;
    private final ParserTemplateConfigMapper templateConfigMapper;

    public RssFeedFetchService(
            RssFeedRepository feedRepository,
            RssArticleRepository articleRepository,
            RssFeedFetchLogRepository fetchLogRepository,
            RssFeedParser feedParser,
            ParserTemplateConfigMapper templateConfigMapper) {
        this.feedRepository = feedRepository;
        this.articleRepository = articleRepository;
        this.fetchLogRepository = fetchLogRepository;
        this.feedParser = feedParser;
        this.templateConfigMapper = templateConfigMapper;
    }

    /**
     * 抓取指定 RSS 源并保存新文章。
     *
     * @param feedUrl RSS 或 Atom 订阅地址
     * @return 抓取结果
     */
    @Transactional
    public RssFetchResult fetch(String feedUrl) {
        OffsetDateTime startedAt = now();
        RssFeed feed = feedRepository.findByFeedUrl(feedUrl)
                .orElseGet(() -> RssFeed.create(feedUrl, feedUrl, DEFAULT_FETCH_INTERVAL_MINUTES, startedAt));
        return fetchExistingFeed(feed, feedUrl, startedAt);
    }

    /**
     * 按订阅源主键抓取并保存新文章。
     *
     * @param feedId 订阅源主键
     * @return 抓取结果
     */
    @Transactional
    public RssFetchResult fetchByFeedId(Long feedId) {
        OffsetDateTime startedAt = now();
        RssFeed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new ResourceNotFoundException("订阅源不存在：" + feedId));
        return fetchExistingFeed(feed, feed.getFeedUrl(), startedAt);
    }

    /**
     * 抓取已存在或新建的订阅源。
     *
     * @param feed 订阅源实体
     * @param feedUrl RSS 或 Atom 地址
     * @param startedAt 抓取开始时间
     * @return 抓取结果
     */
    private RssFetchResult fetchExistingFeed(RssFeed feed, String feedUrl, OffsetDateTime startedAt) {
        feed.markFetchStarted(startedAt);
        feed = feedRepository.save(feed);

        RssFeedFetchLog fetchLog = fetchLogRepository.save(RssFeedFetchLog.start(feed, feedUrl, startedAt));
        try {
            ParsedFeed parsedFeed = parseWithBoundTemplate(feed, feedUrl);
            OffsetDateTime finishedAt = now();
            feed.refreshMetadata(
                    parsedFeed.title(),
                    parsedFeed.siteUrl(),
                    parsedFeed.description(),
                    parsedFeed.language(),
                    finishedAt);

            SaveCounter saveCounter = saveArticles(feed, parsedFeed, finishedAt);
            feed.markFetchSucceeded(finishedAt);
            fetchLog.markSucceeded(
                    parsedFeed.items().size(),
                    saveCounter.newCount(),
                    saveCounter.duplicateCount(),
                    saveCounter.failedCount(),
                    finishedAt);
            return RssFetchResult.success(
                    feedUrl,
                    parsedFeed.items().size(),
                    saveCounter.newCount(),
                    saveCounter.duplicateCount(),
                    saveCounter.failedCount());
        } catch (Exception exception) {
            OffsetDateTime finishedAt = now();
            feed.markFetchFailed(finishedAt);
            fetchLog.markFailed(exception.getMessage(), stackTrace(exception), finishedAt);
            return RssFetchResult.failure(feedUrl, exception.getMessage());
        }
    }

    /**
     * 按绑定模板解析订阅源。
     *
     * @param feed 订阅源实体
     * @param feedUrl RSS 或 Atom 地址
     * @return 解析结果
     */
    private ParsedFeed parseWithBoundTemplate(RssFeed feed, String feedUrl) {
        if (feed.getParserTemplate() == null || !feed.getParserTemplate().isEnabled()) {
            return feedParser.parse(feedUrl);
        }
        ParserTemplateConfig config = templateConfigMapper.fromEntity(feed.getParserTemplate());
        return feedParser.parse(feedUrl, config);
    }

    /**
     * 保存文章并统计新增、重复和失败数量。
     *
     * @param feed 订阅源实体
     * @param parsedFeed 解析结果
     * @param now 当前业务时间
     * @return 保存统计
     */
    private SaveCounter saveArticles(RssFeed feed, ParsedFeed parsedFeed, OffsetDateTime now) {
        int newCount = 0;
        int duplicateCount = 0;
        int failedCount = 0;
        for (ParsedArticleItem item : parsedFeed.items()) {
            try {
                if (isDuplicate(feed.getId(), item)) {
                    duplicateCount++;
                    continue;
                }
                articleRepository.save(RssArticle.create(
                        feed,
                        item.guid(),
                        item.articleUrl(),
                        item.title(),
                        item.summary(),
                        item.contentHtml(),
                        item.author(),
                        item.publishedAt(),
                        item.coverImageUrl(),
                        item.fingerprint(),
                        item.rawPayload(),
                        item.parseTrace(),
                        item.customFields(),
                        item.readingMinutes(),
                        item.wordCount(),
                        now));
                newCount++;
            } catch (Exception exception) {
                failedCount++;
            }
        }
        return new SaveCounter(newCount, duplicateCount, failedCount);
    }

    /**
     * 判断文章是否已经存在，按 GUID、URL、fingerprint 顺序进行去重。
     *
     * @param feedId 订阅源主键
     * @param item 标准化文章条目
     * @return 已存在时返回 true
     */
    private boolean isDuplicate(Long feedId, ParsedArticleItem item) {
        if (item.guid() != null && articleRepository.existsByFeedIdAndGuid(feedId, item.guid())) {
            return true;
        }
        return articleRepository.existsByFeedIdAndArticleUrl(feedId, item.articleUrl())
                || articleRepository.existsByFeedIdAndFingerprint(feedId, item.fingerprint());
    }

    /**
     * 获取当前 UTC 时间。
     *
     * @return 当前 UTC 时间
     */
    private OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    /**
     * 将异常堆栈转换成字符串，便于保存到抓取日志。
     *
     * @param exception 异常对象
     * @return 异常堆栈字符串
     */
    private String stackTrace(Exception exception) {
        StringWriter writer = new StringWriter();
        exception.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    private record SaveCounter(int newCount, int duplicateCount, int failedCount) {
    }
}
