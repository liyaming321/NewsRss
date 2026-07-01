package com.newsrss.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.newsrss.domain.enums.FeedHealthStatus;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class RssFeedTest {

    /**
     * 验证连续失败会先进入 WARNING，再进入 ERROR，成功后恢复 HEALTHY。
     */
    @Test
    void shouldUpdateHealthStatusByConsecutiveFailures() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        RssFeed feed = RssFeed.create("Feed", "https://example.com/rss.xml", 60, now);

        feed.markFetchFailed(now.plusMinutes(1));
        assertThat(feed.getHealthStatus()).isEqualTo(FeedHealthStatus.WARNING);
        assertThat(feed.getConsecutiveFailureCount()).isEqualTo(1);

        feed.markFetchFailed(now.plusMinutes(2));
        assertThat(feed.getHealthStatus()).isEqualTo(FeedHealthStatus.WARNING);
        assertThat(feed.getConsecutiveFailureCount()).isEqualTo(2);

        feed.markFetchFailed(now.plusMinutes(3));
        assertThat(feed.getHealthStatus()).isEqualTo(FeedHealthStatus.ERROR);
        assertThat(feed.getConsecutiveFailureCount()).isEqualTo(3);

        feed.markFetchSucceeded(now.plusMinutes(4));
        assertThat(feed.getHealthStatus()).isEqualTo(FeedHealthStatus.HEALTHY);
        assertThat(feed.getConsecutiveFailureCount()).isZero();
    }
}
