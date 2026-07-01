package com.newsrss.service.feed;

import com.newsrss.common.ResourceNotFoundException;
import com.newsrss.domain.entity.RssFeed;
import com.newsrss.domain.entity.RssParserTemplate;
import com.newsrss.dto.RssFetchResult;
import com.newsrss.dto.common.PageResponse;
import com.newsrss.dto.feed.FeedDetectResponse;
import com.newsrss.dto.feed.FeedRequest;
import com.newsrss.dto.feed.FeedResponse;
import com.newsrss.repository.RssArticleRepository;
import com.newsrss.repository.RssFeedRepository;
import com.newsrss.repository.RssParserTemplateRepository;
import com.newsrss.service.dictionary.DictionaryService;
import com.newsrss.service.rss.ParsedFeed;
import com.newsrss.service.rss.RssFeedFetchService;
import com.newsrss.service.rss.RssFeedParser;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 订阅源服务，负责订阅源 CRUD、探测和手动刷新。
 */
@Service
@Profile("db")
public class FeedService {

    private static final int DEFAULT_FETCH_INTERVAL_MINUTES = 60;

    private final RssFeedRepository feedRepository;
    private final RssParserTemplateRepository templateRepository;
    private final RssArticleRepository articleRepository;
    private final RssFeedParser feedParser;
    private final RssFeedFetchService fetchService;
    private final DictionaryService dictionaryService;

    public FeedService(
            RssFeedRepository feedRepository,
            RssParserTemplateRepository templateRepository,
            RssArticleRepository articleRepository,
            RssFeedParser feedParser,
            RssFeedFetchService fetchService,
            DictionaryService dictionaryService) {
        this.feedRepository = feedRepository;
        this.templateRepository = templateRepository;
        this.articleRepository = articleRepository;
        this.feedParser = feedParser;
        this.fetchService = fetchService;
        this.dictionaryService = dictionaryService;
    }

    /**
     * 分页查询订阅源。
     *
     * @param page 页码
     * @param size 每页条数
     * @return 订阅源分页响应
     */
    @Transactional(readOnly = true)
    public PageResponse<FeedResponse> listFeeds(int page, int size) {
        Page<RssFeed> feedPage = feedRepository.findAll(
                PageRequest.of(normalizePage(page), normalizeSize(size), Sort.by(Sort.Direction.DESC, "createdAt")));
        return new PageResponse<>(
                feedPage.stream().map(this::toResponse).toList(),
                feedPage.getNumber(),
                feedPage.getSize(),
                feedPage.getTotalElements(),
                feedPage.getTotalPages());
    }

    /**
     * 创建订阅源。
     *
     * @param request 保存请求
     * @return 创建后的订阅源
     */
    @Transactional
    public FeedResponse createFeed(FeedRequest request) {
        String feedUrl = normalizeRequired(request.feedUrl(), "RSS 地址不能为空");
        feedRepository.findByFeedUrl(feedUrl).ifPresent(feed -> {
            throw new IllegalArgumentException("订阅源已存在：" + feedUrl);
        });
        String category = normalizeBlank(request.category());
        dictionaryService.validateEnabledFeedCategory(category);
        OffsetDateTime now = now();
        RssFeed feed = RssFeed.create(
                normalizeRequired(request.feedName(), "订阅源名称不能为空"),
                feedUrl,
                request.fetchIntervalMinutes() == null ? DEFAULT_FETCH_INTERVAL_MINUTES : request.fetchIntervalMinutes(),
                now);
        feed.updateManualConfig(
                normalizeRequired(request.feedName(), "订阅源名称不能为空"),
                feedUrl,
                category,
                normalizeBlank(request.iconUrl()),
                request.fetchIntervalMinutes() == null ? DEFAULT_FETCH_INTERVAL_MINUTES : request.fetchIntervalMinutes(),
                request.enabled() == null || request.enabled(),
                now);
        feed.bindParserTemplate(resolveTemplate(request.parserTemplateId()), now);
        return toResponse(feedRepository.save(feed));
    }

    /**
     * 更新订阅源。
     *
     * @param id 订阅源主键
     * @param request 保存请求
     * @return 更新后的订阅源
     */
    @Transactional
    public FeedResponse updateFeed(Long id, FeedRequest request) {
        RssFeed feed = findFeed(id);
        String feedUrl = normalizeRequired(request.feedUrl(), "RSS 地址不能为空");
        feedRepository.findByFeedUrl(feedUrl)
                .filter(existingFeed -> !existingFeed.getId().equals(id))
                .ifPresent(existingFeed -> {
                    throw new IllegalArgumentException("订阅源已存在：" + feedUrl);
                });
        String category = normalizeBlank(request.category());
        dictionaryService.validateEnabledFeedCategory(category);
        OffsetDateTime now = now();
        feed.updateManualConfig(
                normalizeRequired(request.feedName(), "订阅源名称不能为空"),
                feedUrl,
                category,
                normalizeBlank(request.iconUrl()),
                request.fetchIntervalMinutes() == null ? DEFAULT_FETCH_INTERVAL_MINUTES : request.fetchIntervalMinutes(),
                request.enabled() == null || request.enabled(),
                now);
        feed.bindParserTemplate(resolveTemplate(request.parserTemplateId()), now);
        return toResponse(feed);
    }

    /**
     * 删除订阅源。
     *
     * @param id 订阅源主键
     */
    @Transactional
    public void deleteFeed(Long id) {
        feedRepository.delete(findFeed(id));
    }

    /**
     * 手动刷新订阅源。
     *
     * @param id 订阅源主键
     * @return 抓取结果
     */
    @Transactional
    public RssFetchResult refreshFeed(Long id) {
        return fetchService.fetchByFeedId(id);
    }

    /**
     * 探测订阅源元数据。
     *
     * @param feedUrl RSS 或 Atom 地址
     * @return 探测结果
     */
    @Transactional(readOnly = true)
    public FeedDetectResponse detectFeed(String feedUrl) {
        String normalizedFeedUrl = normalizeRequired(feedUrl, "RSS 地址不能为空");
        ParsedFeed parsedFeed = feedParser.parse(normalizedFeedUrl);
        return new FeedDetectResponse(
                normalizedFeedUrl,
                parsedFeed.title(),
                parsedFeed.siteUrl(),
                parsedFeed.description(),
                parsedFeed.language(),
                parsedFeed.items().size());
    }

    /**
     * 查询订阅源实体。
     *
     * @param id 订阅源主键
     * @return 订阅源实体
     */
    @Transactional(readOnly = true)
    public RssFeed findFeed(Long id) {
        return feedRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("订阅源不存在：" + id));
    }

    /**
     * 转换订阅源响应。
     *
     * @param feed 订阅源实体
     * @return 订阅源响应
     */
    public FeedResponse toResponse(RssFeed feed) {
        RssParserTemplate template = feed.getParserTemplate();
        return new FeedResponse(
                feed.getId(),
                template == null ? null : template.getId(),
                template == null ? null : template.getTemplateCode(),
                feed.getFeedName(),
                feed.getFeedUrl(),
                feed.getSiteUrl(),
                feed.getDescription(),
                feed.getLanguage(),
                feed.getCategory(),
                feed.getIconUrl(),
                feed.getHealthStatus().name(),
                feed.isEnabled(),
                feed.getFetchIntervalMinutes(),
                feed.getLastFetchAt(),
                feed.getNextFetchAt(),
                feed.getLastSuccessAt(),
                feed.getLastFailureAt(),
                feed.getConsecutiveFailureCount(),
                articleRepository.countByFeedId(feed.getId()),
                feed.getCreatedAt(),
                feed.getUpdatedAt());
    }

    /**
     * 查询模板实体。
     *
     * @param parserTemplateId 模板主键
     * @return 模板实体，主键为空时返回 null
     */
    private RssParserTemplate resolveTemplate(Long parserTemplateId) {
        if (parserTemplateId == null) {
            return null;
        }
        RssParserTemplate template = templateRepository.findById(parserTemplateId)
                .orElseThrow(() -> new ResourceNotFoundException("解析模板不存在：" + parserTemplateId));
        if (!template.isEnabled()) {
            throw new IllegalArgumentException("解析模板已停用，不能绑定：" + template.getTemplateCode());
        }
        return template;
    }

    /**
     * 归一化页码。
     *
     * @param page 页码
     * @return 合法页码
     */
    private int normalizePage(int page) {
        return Math.max(page, 0);
    }

    /**
     * 归一化每页条数。
     *
     * @param size 每页条数
     * @return 合法每页条数
     */
    private int normalizeSize(int size) {
        return Math.max(1, Math.min(size, 100));
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
     * 归一化必填字符串。
     *
     * @param value 原始字符串
     * @param message 缺失提示
     * @return 去除首尾空白后的字符串
     */
    private String normalizeRequired(String value, String message) {
        String normalizedValue = normalizeBlank(value);
        if (normalizedValue == null) {
            throw new IllegalArgumentException(message);
        }
        return normalizedValue;
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
