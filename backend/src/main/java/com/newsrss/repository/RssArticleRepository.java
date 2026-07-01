package com.newsrss.repository;

import com.newsrss.domain.entity.RssArticle;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * RSS 文章数据访问接口。
 */
public interface RssArticleRepository extends JpaRepository<RssArticle, Long> {

    /**
     * 按订阅源和文章链接查询文章。
     *
     * @param feedId 订阅源主键
     * @param articleUrl 文章链接
     * @return 匹配的文章
     */
    Optional<RssArticle> findByFeedIdAndArticleUrl(Long feedId, String articleUrl);

    /**
     * 按订阅源和去重指纹查询文章。
     *
     * @param feedId 订阅源主键
     * @param fingerprint 文章去重指纹
     * @return 匹配的文章
     */
    Optional<RssArticle> findByFeedIdAndFingerprint(Long feedId, String fingerprint);

    /**
     * 判断订阅源下是否已存在同链接文章。
     *
     * @param feedId 订阅源主键
     * @param articleUrl 文章链接
     * @return 如果存在则返回 true
     */
    boolean existsByFeedIdAndArticleUrl(Long feedId, String articleUrl);

    /**
     * 判断订阅源下是否已存在同去重指纹文章。
     *
     * @param feedId 订阅源主键
     * @param fingerprint 文章去重指纹
     * @return 如果存在则返回 true
     */
    boolean existsByFeedIdAndFingerprint(Long feedId, String fingerprint);

    /**
     * 判断订阅源下是否已存在同 GUID 文章。
     *
     * @param feedId 订阅源主键
     * @param guid RSS 条目 GUID
     * @return 如果存在则返回 true
     */
    boolean existsByFeedIdAndGuid(Long feedId, String guid);

    /**
     * 分页查询文章，支持按订阅源、阅读筛选和关键词过滤。
     *
     * @param feedId 订阅源主键，可为空
     * @param userKey 用户标识
     * @param filter 筛选类型
     * @param keyword 搜索关键词
     * @param todayStart 今日起始时间
     * @param pageable 分页参数
     * @return 文章分页
     */
    @Query("""
            select article
            from RssArticle article
            left join RssArticleUserState state
                on state.article = article and state.userKey = :userKey
            where (:feedId is null or article.feed.id = :feedId)
                and (
                    :filter = 'ALL'
                    or (:filter = 'UNREAD' and (state is null or state.read = false))
                    or (:filter = 'FAVORITE' and state.favorite = true)
                    or (:filter = 'READ_LATER' and state.readLater = true)
                    or (:filter = 'TODAY' and article.fetchedAt >= :todayStart)
                )
                and (
                    :keyword = ''
                    or lower(article.title) like concat('%', :keyword, '%')
                    or lower(article.feed.feedName) like concat('%', :keyword, '%')
                    or lower(coalesce(article.summary, '')) like concat('%', :keyword, '%')
                )
            order by coalesce(article.publishedAt, article.fetchedAt) desc
            """)
    Page<RssArticle> findArticles(
            @Param("feedId") Long feedId,
            @Param("userKey") String userKey,
            @Param("filter") String filter,
            @Param("keyword") String keyword,
            @Param("todayStart") OffsetDateTime todayStart,
            Pageable pageable);

    /**
     * 统计指定筛选条件下的文章数量。
     *
     * @param feedId 订阅源主键，可为空
     * @param userKey 用户标识
     * @param filter 筛选类型
     * @param keyword 搜索关键词
     * @param todayStart 今日起始时间
     * @return 匹配文章数量
     */
    @Query("""
            select count(article)
            from RssArticle article
            left join RssArticleUserState state
                on state.article = article and state.userKey = :userKey
            where (:feedId is null or article.feed.id = :feedId)
                and (
                    :filter = 'ALL'
                    or (:filter = 'UNREAD' and (state is null or state.read = false))
                    or (:filter = 'FAVORITE' and state.favorite = true)
                    or (:filter = 'READ_LATER' and state.readLater = true)
                    or (:filter = 'TODAY' and article.fetchedAt >= :todayStart)
                )
                and (
                    :keyword = ''
                    or lower(article.title) like concat('%', :keyword, '%')
                    or lower(article.feed.feedName) like concat('%', :keyword, '%')
                    or lower(coalesce(article.summary, '')) like concat('%', :keyword, '%')
                )
            """)
    long countArticles(
            @Param("feedId") Long feedId,
            @Param("userKey") String userKey,
            @Param("filter") String filter,
            @Param("keyword") String keyword,
            @Param("todayStart") OffsetDateTime todayStart);

    /**
     * 按订阅源统计指定筛选条件下的文章数量。
     *
     * @param userKey 用户标识
     * @param filter 筛选类型
     * @param keyword 搜索关键词
     * @param todayStart 今日起始时间
     * @return 订阅源主键、订阅源名称和文章数量
     */
    @Query("""
            select article.feed.id, article.feed.feedName, count(article)
            from RssArticle article
            left join RssArticleUserState state
                on state.article = article and state.userKey = :userKey
            where (
                    :filter = 'ALL'
                    or (:filter = 'UNREAD' and (state is null or state.read = false))
                    or (:filter = 'FAVORITE' and state.favorite = true)
                    or (:filter = 'READ_LATER' and state.readLater = true)
                    or (:filter = 'TODAY' and article.fetchedAt >= :todayStart)
                )
                and (
                    :keyword = ''
                    or lower(article.title) like concat('%', :keyword, '%')
                    or lower(article.feed.feedName) like concat('%', :keyword, '%')
                    or lower(coalesce(article.summary, '')) like concat('%', :keyword, '%')
                )
            group by article.feed.id, article.feed.feedName
            order by count(article) desc, article.feed.feedName asc
            """)
    List<Object[]> countArticlesGroupByFeed(
            @Param("userKey") String userKey,
            @Param("filter") String filter,
            @Param("keyword") String keyword,
            @Param("todayStart") OffsetDateTime todayStart);

    /**
     * 统计指定订阅源文章数量。
     *
     * @param feedId 订阅源主键
     * @return 文章数量
     */
    long countByFeedId(Long feedId);

    /**
     * 统计所有文章数量。
     *
     * @return 文章数量
     */
    long countBy();

    /**
     * 统计指定抓取时间之后入库的文章数量。
     *
     * @param fetchedAt 抓取时间下限
     * @return 文章数量
     */
    long countByFetchedAtGreaterThanEqual(OffsetDateTime fetchedAt);

    /**
     * 按入库时间查询最近文章。
     *
     * @param pageable 分页参数
     * @return 最近入库文章列表
     */
    @Query("""
            select article
            from RssArticle article
            order by article.fetchedAt desc, article.id desc
            """)
    Page<RssArticle> findRecentlyFetchedArticles(Pageable pageable);

    /**
     * 按主键加锁查询文章，用于串行化同一文章的状态更新。
     *
     * @param id 文章主键
     * @return 加锁后的文章实体
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select article from RssArticle article where article.id = :id")
    Optional<RssArticle> findByIdForUpdate(@Param("id") Long id);
}
