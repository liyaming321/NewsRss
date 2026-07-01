package com.newsrss.service.article;

import com.newsrss.common.ResourceNotFoundException;
import com.newsrss.domain.entity.RssArticle;
import com.newsrss.domain.entity.RssArticleUserState;
import com.newsrss.dto.article.ArticleFlagRequest;
import com.newsrss.dto.article.ArticleFeedStatResponse;
import com.newsrss.dto.article.ArticleFilter;
import com.newsrss.dto.article.ArticleListItemResponse;
import com.newsrss.dto.article.ArticleResponse;
import com.newsrss.dto.article.ArticleStateResponse;
import com.newsrss.dto.article.ArticleStatsResponse;
import com.newsrss.dto.common.PageResponse;
import com.newsrss.repository.RssArticleRepository;
import com.newsrss.repository.RssArticleUserStateRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 文章服务，负责文章查询和阅读状态更新。
 */
@Service
@Profile("db")
public class ArticleService {

    private static final String DEFAULT_USER_KEY = "default";

    private final RssArticleRepository articleRepository;
    private final RssArticleUserStateRepository stateRepository;

    public ArticleService(
            RssArticleRepository articleRepository,
            RssArticleUserStateRepository stateRepository) {
        this.articleRepository = articleRepository;
        this.stateRepository = stateRepository;
    }

    /**
     * 分页查询文章。
     *
     * @param feedId 订阅源主键
     * @param filterText 筛选文本
     * @param keyword 搜索关键词
     * @param page 页码
     * @param size 每页条数
     * @return 文章分页
     */
    @Transactional(readOnly = true)
    public PageResponse<ArticleListItemResponse> listArticles(Long feedId, String filterText, String keyword, int page, int size) {
        ArticleFilter filter = ArticleFilter.from(filterText);
        Page<RssArticle> articlePage = articleRepository.findArticles(
                feedId,
                DEFAULT_USER_KEY,
                filter.name(),
                normalizeKeyword(keyword),
                todayStart(),
                PageRequest.of(normalizePage(page), normalizeSize(size)));
        return new PageResponse<>(
                articlePage.stream().map(this::toListItemResponse).toList(),
                articlePage.getNumber(),
                articlePage.getSize(),
                articlePage.getTotalElements(),
                articlePage.getTotalPages());
    }

    /**
     * 查询文章统计，供阅读页过滤器显示全量数量。
     *
     * @param feedId 订阅源主键
     * @param filterText 筛选文本
     * @param keyword 搜索关键词
     * @return 文章统计响应
     */
    @Transactional(readOnly = true)
    public ArticleStatsResponse getArticleStats(Long feedId, String filterText, String keyword) {
        ArticleFilter filter = ArticleFilter.from(filterText);
        String normalizedKeyword = normalizeKeyword(keyword);
        OffsetDateTime todayStart = todayStart();
        List<ArticleFeedStatResponse> feedStats = articleRepository.countArticlesGroupByFeed(DEFAULT_USER_KEY, filter.name(), normalizedKeyword, todayStart)
                .stream()
                .map(row -> new ArticleFeedStatResponse(
                        (Long) row[0],
                        (String) row[1],
                        ((Number) row[2]).longValue()))
                .toList();
        return new ArticleStatsResponse(
                articleRepository.countArticles(feedId, DEFAULT_USER_KEY, ArticleFilter.ALL.name(), normalizedKeyword, todayStart),
                articleRepository.countArticles(feedId, DEFAULT_USER_KEY, ArticleFilter.UNREAD.name(), normalizedKeyword, todayStart),
                articleRepository.countArticles(feedId, DEFAULT_USER_KEY, ArticleFilter.FAVORITE.name(), normalizedKeyword, todayStart),
                articleRepository.countArticles(feedId, DEFAULT_USER_KEY, ArticleFilter.READ_LATER.name(), normalizedKeyword, todayStart),
                articleRepository.countArticles(feedId, DEFAULT_USER_KEY, ArticleFilter.TODAY.name(), normalizedKeyword, todayStart),
                feedStats);
    }

    /**
     * 查询文章详情。
     *
     * @param id 文章主键
     * @return 文章详情
     */
    @Transactional(readOnly = true)
    public ArticleResponse getArticle(Long id) {
        return toResponse(findArticle(id));
    }

    /**
     * 更新已读状态。
     *
     * @param id 文章主键
     * @param request 状态请求
     * @return 更新后的文章状态
     */
    @Transactional
    public ArticleStateResponse updateReadState(Long id, ArticleFlagRequest request) {
        RssArticleUserState state = findOrCreateState(findArticleForUpdate(id));
        state.setRead(requireValue(request), now());
        return toStateResponse(state);
    }

    /**
     * 更新收藏状态。
     *
     * @param id 文章主键
     * @param request 状态请求
     * @return 更新后的文章状态
     */
    @Transactional
    public ArticleStateResponse updateFavorite(Long id, ArticleFlagRequest request) {
        RssArticleUserState state = findOrCreateState(findArticleForUpdate(id));
        state.setFavorite(requireValue(request), now());
        return toStateResponse(state);
    }

    /**
     * 更新稍后读状态。
     *
     * @param id 文章主键
     * @param request 状态请求
     * @return 更新后的文章状态
     */
    @Transactional
    public ArticleStateResponse updateReadLater(Long id, ArticleFlagRequest request) {
        RssArticleUserState state = findOrCreateState(findArticleForUpdate(id));
        state.setReadLater(requireValue(request), now());
        return toStateResponse(state);
    }

    /**
     * 更新归档状态。
     *
     * @param id 文章主键
     * @param request 状态请求
     * @return 更新后的文章状态
     */
    @Transactional
    public ArticleStateResponse updateArchive(Long id, ArticleFlagRequest request) {
        RssArticleUserState state = findOrCreateState(findArticleForUpdate(id));
        state.setArchived(requireValue(request), now());
        return toStateResponse(state);
    }

    /**
     * 查询文章实体。
     *
     * @param id 文章主键
     * @return 文章实体
     */
    @Transactional(readOnly = true)
    public RssArticle findArticle(Long id) {
        return articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("文章不存在：" + id));
    }

    /**
     * 加锁查询文章实体，用于串行化状态更新。
     *
     * @param id 文章主键
     * @return 文章实体
     */
    private RssArticle findArticleForUpdate(Long id) {
        return articleRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("文章不存在：" + id));
    }

    /**
     * 转换文章列表项响应。
     *
     * @param article 文章实体
     * @return 文章列表项响应
     */
    public ArticleListItemResponse toListItemResponse(RssArticle article) {
        return new ArticleListItemResponse(
                article.getId(),
                article.getFeed().getId(),
                article.getFeed().getFeedName(),
                article.getTitle(),
                article.getArticleUrl(),
                article.getSummary(),
                article.getAuthor(),
                article.getPublishedAt(),
                article.getFetchedAt(),
                article.getCoverImageUrl(),
                article.getReadingMinutes(),
                article.getWordCount(),
                toStateResponse(findState(article).orElse(null)));
    }

    /**
     * 转换文章详情响应。
     *
     * @param article 文章实体
     * @return 文章详情响应
     */
    public ArticleResponse toResponse(RssArticle article) {
        return new ArticleResponse(
                article.getId(),
                article.getFeed().getId(),
                article.getFeed().getFeedName(),
                article.getGuid(),
                article.getArticleUrl(),
                article.getCanonicalUrl(),
                article.getTitle(),
                article.getSummary(),
                article.getContentHtml(),
                article.getAuthor(),
                article.getPublishedAt(),
                article.getFetchedAt(),
                article.getCoverImageUrl(),
                article.getReadingMinutes(),
                article.getWordCount(),
                article.getRawPayload(),
                article.getParseTrace(),
                article.getCustomFields(),
                toStateResponse(findState(article).orElse(null)));
    }

    /**
     * 转换文章状态响应。
     *
     * @param state 文章用户状态
     * @return 文章状态响应
     */
    public ArticleStateResponse toStateResponse(RssArticleUserState state) {
        if (state == null) {
            return new ArticleStateResponse(false, false, false, false, null, null, null, null);
        }
        return new ArticleStateResponse(
                state.isRead(),
                state.isFavorite(),
                state.isReadLater(),
                state.isArchived(),
                state.getReadAt(),
                state.getFavoritedAt(),
                state.getReadLaterAt(),
                state.getArchivedAt());
    }

    /**
     * 查找或创建文章状态。
     *
     * @param article 文章实体
     * @return 文章状态
     */
    private RssArticleUserState findOrCreateState(RssArticle article) {
        return findState(article)
                .orElseGet(() -> stateRepository.save(RssArticleUserState.create(article, DEFAULT_USER_KEY, now())));
    }

    /**
     * 查询文章状态。
     *
     * @param article 文章实体
     * @return 文章状态
     */
    private java.util.Optional<RssArticleUserState> findState(RssArticle article) {
        return stateRepository.findByArticleIdAndUserKey(article.getId(), DEFAULT_USER_KEY);
    }

    /**
     * 获取布尔状态值。
     *
     * @param request 状态请求
     * @return 状态值
     */
    private boolean requireValue(ArticleFlagRequest request) {
        if (request == null || request.value() == null) {
            throw new IllegalArgumentException("状态值不能为空");
        }
        return request.value();
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
     * 归一化搜索关键词。
     *
     * @param keyword 原始关键词
     * @return 小写关键词，空值时返回空字符串
     */
    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return "";
        }
        return keyword.strip().toLowerCase();
    }

    /**
     * 获取当前自然日的 UTC 起始时间。
     *
     * @return 当前自然日 UTC 零点
     */
    private OffsetDateTime todayStart() {
        return LocalDate.now(ZoneOffset.UTC).atStartOfDay().atOffset(ZoneOffset.UTC);
    }

    /**
     * 获取当前 UTC 时间。
     *
     * @return 当前 UTC 时间
     */
    private OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }
}
