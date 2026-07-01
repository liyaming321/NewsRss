package com.newsrss.service.feed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.newsrss.config.RssFetchSchedulerProperties;
import com.newsrss.domain.entity.RssFeed;
import com.newsrss.dto.RssFetchResult;
import com.newsrss.repository.RssFeedRepository;
import com.newsrss.service.rss.RssFeedFetchService;
import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Pageable;

class RssFeedScheduleServiceTest {

    private final RssFeedRepository feedRepository = Mockito.mock(RssFeedRepository.class);
    private final RssFeedFetchService fetchService = Mockito.mock(RssFeedFetchService.class);
    private final RssFetchSchedulerProperties properties = new RssFetchSchedulerProperties();
    private final RssFeedScheduleService scheduleService = new RssFeedScheduleService(
            feedRepository,
            fetchService,
            properties);

    /**
     * 验证调度只抓取到期源并汇总结果。
     */
    @Test
    void shouldRunDueFeeds() throws Exception {
        properties.setBatchSize(2);
        RssFeed feed = RssFeed.create("Feed", "https://example.com/rss.xml", 60, now());
        setId(feed, 7L);
        when(feedRepository.findDueFeeds(any(OffsetDateTime.class), any(Pageable.class))).thenReturn(List.of(feed));
        when(fetchService.fetchByFeedId(7L)).thenReturn(RssFetchResult.success(feed.getFeedUrl(), 2, 1, 1, 0));

        var response = scheduleService.runDueFeeds();

        assertThat(response.dueCount()).isEqualTo(1);
        assertThat(response.successCount()).isEqualTo(1);
        assertThat(response.failureCount()).isEqualTo(0);
        assertThat(response.results()).hasSize(1);
    }

    /**
     * 验证批量刷新指定订阅源。
     */
    @Test
    void shouldRefreshBatchByIds() throws Exception {
        RssFeed feed = RssFeed.create("Feed", "https://example.com/rss.xml", 60, now());
        setId(feed, 8L);
        when(feedRepository.findAllById(List.of(8L))).thenReturn(List.of(feed));
        when(fetchService.fetchByFeedId(8L)).thenReturn(RssFetchResult.failure(feed.getFeedUrl(), "抓取失败"));

        var response = scheduleService.refreshBatch(List.of(8L));

        assertThat(response.requestedCount()).isEqualTo(1);
        assertThat(response.successCount()).isEqualTo(0);
        assertThat(response.failureCount()).isEqualTo(1);
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
     * 设置测试实体主键。
     *
     * @param feed 订阅源实体
     * @param id 主键
     */
    private void setId(RssFeed feed, Long id) throws Exception {
        Field idField = RssFeed.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(feed, id);
    }
}
