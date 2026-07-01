package com.newsrss.service.feed;

import com.newsrss.config.RssFetchSchedulerProperties;
import com.newsrss.domain.entity.RssFeed;
import com.newsrss.dto.RssFetchResult;
import com.newsrss.dto.feed.FeedBatchRefreshResponse;
import com.newsrss.dto.feed.FeedScheduleRunResponse;
import com.newsrss.repository.RssFeedRepository;
import com.newsrss.service.rss.RssFeedFetchService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * RSS 抓取调度服务，负责定时刷新、批量刷新和调度手动触发。
 */
@Service
@Profile("db")
public class RssFeedScheduleService {

    private final RssFeedRepository feedRepository;
    private final RssFeedFetchService fetchService;
    private final RssFetchSchedulerProperties properties;

    public RssFeedScheduleService(
            RssFeedRepository feedRepository,
            RssFeedFetchService fetchService,
            RssFetchSchedulerProperties properties) {
        this.feedRepository = feedRepository;
        this.fetchService = fetchService;
        this.properties = properties;
    }

    /**
     * 定时抓取到期订阅源。
     */
    @Scheduled(
            fixedDelayString = "${newsrss.fetch.scheduler.fixed-delay-ms:60000}",
            initialDelayString = "${newsrss.fetch.scheduler.initial-delay-ms:15000}")
    public void runScheduledFetch() {
        if (!properties.isEnabled()) {
            return;
        }
        runDueFeeds();
    }

    /**
     * 手动触发一次到期源调度。
     *
     * @return 调度执行结果
     */
    public FeedScheduleRunResponse runDueFeeds() {
        OffsetDateTime triggeredAt = now();
        List<RssFeed> dueFeeds = feedRepository.findDueFeeds(
                triggeredAt,
                PageRequest.of(0, normalizeBatchSize(properties.getBatchSize())));
        List<RssFetchResult> results = dueFeeds.stream()
                .map(feed -> fetchService.fetchByFeedId(feed.getId()))
                .toList();
        return new FeedScheduleRunResponse(
                triggeredAt,
                dueFeeds.size(),
                countSuccess(results),
                countFailure(results),
                results);
    }

    /**
     * 批量刷新订阅源。
     *
     * @param feedIds 指定订阅源主键列表，为空时刷新全部启用源
     * @return 批量刷新结果
     */
    public FeedBatchRefreshResponse refreshBatch(List<Long> feedIds) {
        List<RssFeed> feeds = resolveFeeds(feedIds);
        List<RssFetchResult> results = feeds.stream()
                .map(feed -> fetchService.fetchByFeedId(feed.getId()))
                .toList();
        return new FeedBatchRefreshResponse(feeds.size(), countSuccess(results), countFailure(results), results);
    }

    /**
     * 解析批量刷新源列表。
     *
     * @param feedIds 指定订阅源主键列表
     * @return 订阅源列表
     */
    private List<RssFeed> resolveFeeds(List<Long> feedIds) {
        if (feedIds == null || feedIds.isEmpty()) {
            return feedRepository.findByEnabledTrueOrderByIdAsc();
        }
        return feedRepository.findAllById(feedIds);
    }

    /**
     * 统计成功数量。
     *
     * @param results 抓取结果列表
     * @return 成功数量
     */
    private int countSuccess(List<RssFetchResult> results) {
        return (int) results.stream().filter(RssFetchResult::success).count();
    }

    /**
     * 统计失败数量。
     *
     * @param results 抓取结果列表
     * @return 失败数量
     */
    private int countFailure(List<RssFetchResult> results) {
        return (int) results.stream().filter(result -> !result.success()).count();
    }

    /**
     * 归一化批次大小。
     *
     * @param batchSize 配置批次大小
     * @return 合法批次大小
     */
    private int normalizeBatchSize(int batchSize) {
        return Math.max(1, Math.min(batchSize, 50));
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
