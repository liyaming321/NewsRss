package com.newsrss.repository;

import com.newsrss.domain.entity.RssFeedFetchLog;
import com.newsrss.domain.enums.FetchLogStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

/**
 * RSS 抓取日志数据访问接口。
 */
public interface RssFeedFetchLogRepository
        extends JpaRepository<RssFeedFetchLog, Long>, JpaSpecificationExecutor<RssFeedFetchLog> {

    /**
     * 查询最近抓取日志。
     *
     * @param pageable 分页参数
     * @return 抓取日志分页
     */
    @Query("""
            select fetchLog
            from RssFeedFetchLog fetchLog
            order by fetchLog.startedAt desc
            """)
    Page<RssFeedFetchLog> findRecentLogs(Pageable pageable);

    /**
     * 统计指定状态抓取日志数量。
     *
     * @param status 抓取状态
     * @return 日志数量
     */
    long countByStatus(FetchLogStatus status);
}
