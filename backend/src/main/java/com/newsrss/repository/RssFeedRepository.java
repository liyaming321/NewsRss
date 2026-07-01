package com.newsrss.repository;

import com.newsrss.domain.entity.RssFeed;
import com.newsrss.domain.enums.FeedHealthStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * RSS 订阅源数据访问接口。
 */
public interface RssFeedRepository extends JpaRepository<RssFeed, Long> {

    /**
     * 按订阅地址查询订阅源。
     *
     * @param feedUrl RSS 或 Atom 订阅地址
     * @return 匹配的订阅源
     */
    Optional<RssFeed> findByFeedUrl(String feedUrl);

    /**
     * 统计指定健康状态订阅源数量。
     *
     * @param healthStatus 健康状态
     * @return 订阅源数量
     */
    long countByHealthStatus(FeedHealthStatus healthStatus);

    /**
     * 统计启用订阅源数量。
     *
     * @return 启用订阅源数量
     */
    long countByEnabledTrue();

    /**
     * 查询到期需要抓取的启用订阅源。
     *
     * @param now 当前业务时间
     * @param pageable 分页参数
     * @return 到期订阅源列表
     */
    @Query("""
            select feed
            from RssFeed feed
            where feed.enabled = true
              and (feed.nextFetchAt is null or feed.nextFetchAt <= :now)
            order by feed.nextFetchAt asc, feed.id asc
            """)
    List<RssFeed> findDueFeeds(@Param("now") OffsetDateTime now, Pageable pageable);

    /**
     * 查询全部启用订阅源。
     *
     * @return 启用订阅源列表
     */
    List<RssFeed> findByEnabledTrueOrderByIdAsc();
}
