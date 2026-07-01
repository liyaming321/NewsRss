package com.newsrss.service.dashboard;

import com.newsrss.domain.enums.FeedHealthStatus;
import com.newsrss.domain.enums.FetchLogStatus;
import com.newsrss.dto.article.ArticleListItemResponse;
import com.newsrss.dto.dashboard.DashboardSummaryResponse;
import com.newsrss.dto.dashboard.FeedHealthResponse;
import com.newsrss.repository.RssArticleRepository;
import com.newsrss.repository.RssArticleUserStateRepository;
import com.newsrss.repository.RssFeedFetchLogRepository;
import com.newsrss.repository.RssFeedRepository;
import com.newsrss.service.article.ArticleService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 驾驶舱服务，负责首页统计和最近文章查询。
 */
@Service
@Profile("db")
public class DashboardService {

    private static final String DEFAULT_USER_KEY = "default";

    private final RssFeedRepository feedRepository;
    private final RssArticleRepository articleRepository;
    private final RssArticleUserStateRepository stateRepository;
    private final RssFeedFetchLogRepository fetchLogRepository;
    private final ArticleService articleService;

    public DashboardService(
            RssFeedRepository feedRepository,
            RssArticleRepository articleRepository,
            RssArticleUserStateRepository stateRepository,
            RssFeedFetchLogRepository fetchLogRepository,
            ArticleService articleService) {
        this.feedRepository = feedRepository;
        this.articleRepository = articleRepository;
        this.stateRepository = stateRepository;
        this.fetchLogRepository = fetchLogRepository;
        this.articleService = articleService;
    }

    /**
     * 查询驾驶舱摘要统计。
     *
     * @return 驾驶舱摘要
     */
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        long articleCount = articleRepository.count();
        long readCount = stateRepository.countByUserKeyAndReadTrue(DEFAULT_USER_KEY);
        long archivedCount = stateRepository.countByUserKeyAndArchivedTrue(DEFAULT_USER_KEY);
        OffsetDateTime todayStart = OffsetDateTime.now(ZoneOffset.UTC).toLocalDate().atStartOfDay().atOffset(ZoneOffset.UTC);
        long unreadCount = Math.max(0, articleCount - readCount - archivedCount);
        return new DashboardSummaryResponse(
                feedRepository.count(),
                feedRepository.countByEnabledTrue(),
                articleCount,
                articleRepository.countByFetchedAtGreaterThanEqual(todayStart),
                unreadCount,
                stateRepository.countByUserKeyAndFavoriteTrue(DEFAULT_USER_KEY),
                stateRepository.countByUserKeyAndReadLaterTrue(DEFAULT_USER_KEY),
                fetchLogRepository.countByStatus(FetchLogStatus.FAILED));
    }

    /**
     * 查询订阅源健康状态统计。
     *
     * @return 健康状态统计
     */
    @Transactional(readOnly = true)
    public FeedHealthResponse getFeedHealth() {
        return new FeedHealthResponse(
                feedRepository.countByHealthStatus(FeedHealthStatus.UNKNOWN),
                feedRepository.countByHealthStatus(FeedHealthStatus.HEALTHY),
                feedRepository.countByHealthStatus(FeedHealthStatus.WARNING),
                feedRepository.countByHealthStatus(FeedHealthStatus.ERROR));
    }

    /**
     * 查询最近入库文章。
     *
     * @param limit 返回条数
     * @return 最近入库文章列表
     */
    @Transactional(readOnly = true)
    public List<ArticleListItemResponse> getRecentArticles(int limit) {
        int normalizedLimit = Math.max(1, Math.min(limit, 20));
        return articleRepository.findRecentlyFetchedArticles(PageRequest.of(0, normalizedLimit)).stream()
                .map(articleService::toListItemResponse)
                .toList();
    }
}
