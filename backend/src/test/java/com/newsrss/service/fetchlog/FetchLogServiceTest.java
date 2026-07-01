package com.newsrss.service.fetchlog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.newsrss.domain.entity.RssFeed;
import com.newsrss.domain.entity.RssFeedFetchLog;
import com.newsrss.domain.enums.FetchLogStatus;
import com.newsrss.repository.RssFeedFetchLogRepository;
import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

class FetchLogServiceTest {

    private final RssFeedFetchLogRepository fetchLogRepository = Mockito.mock(RssFeedFetchLogRepository.class);
    private final FetchLogService fetchLogService = new FetchLogService(fetchLogRepository);

    /**
     * 验证日志筛选参数会传递到仓储并正确转换响应。
     */
    @Test
    void shouldListFetchLogsByFilters() throws Exception {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        RssFeed feed = RssFeed.create("Feed", "https://example.com/rss.xml", 60, now);
        setId(feed, 11L);
        RssFeedFetchLog fetchLog = RssFeedFetchLog.start(feed, feed.getFeedUrl(), now);
        setId(fetchLog, 21L);
        fetchLog.markFailed("网络失败", "UnknownHostException", now.plusSeconds(2));
        when(fetchLogRepository.findAll(
                Mockito.<Specification<RssFeedFetchLog>>any(),
                Mockito.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(fetchLog)));

        var response = fetchLogService.listFetchLogs(
                0,
                20,
                11L,
                "failed",
                now.minusHours(1),
                now.plusHours(1));

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).id()).isEqualTo(21L);
        assertThat(response.items().get(0).status()).isEqualTo("FAILED");
        assertThat(response.items().get(0).errorMessage()).isEqualTo("网络失败");
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        Mockito.verify(fetchLogRepository).findAll(
                Mockito.<Specification<RssFeedFetchLog>>any(),
                pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("startedAt")).isNotNull();
    }

    /**
     * 验证非法状态会给出可读异常。
     */
    @Test
    void shouldRejectInvalidStatus() {
        assertThatThrownBy(() -> fetchLogService.listFetchLogs(0, 20, null, "BAD", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("抓取状态不合法");
    }

    /**
     * 设置测试实体主键。
     *
     * @param target 测试实体
     * @param id 主键
     */
    private void setId(Object target, Long id) throws Exception {
        Field idField = target.getClass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(target, id);
    }
}
