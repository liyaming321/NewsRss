package com.newsrss.service.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.newsrss.domain.enums.FetchLogStatus;
import com.newsrss.repository.RssArticleRepository;
import com.newsrss.repository.RssArticleUserStateRepository;
import com.newsrss.repository.RssFeedFetchLogRepository;
import com.newsrss.repository.RssFeedRepository;
import com.newsrss.service.article.ArticleService;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

class DashboardServiceTest {

    private final RssFeedRepository feedRepository = Mockito.mock(RssFeedRepository.class);
    private final RssArticleRepository articleRepository = Mockito.mock(RssArticleRepository.class);
    private final RssArticleUserStateRepository stateRepository = Mockito.mock(RssArticleUserStateRepository.class);
    private final RssFeedFetchLogRepository fetchLogRepository = Mockito.mock(RssFeedFetchLogRepository.class);
    private final ArticleService articleService = Mockito.mock(ArticleService.class);
    private final DashboardService dashboardService = new DashboardService(
            feedRepository,
            articleRepository,
            stateRepository,
            fetchLogRepository,
            articleService);

    /**
     * 验证驾驶舱摘要会把没有状态记录的文章视作未读。
     */
    @Test
    void shouldCountArticlesWithoutStateAsUnread() {
        when(feedRepository.count()).thenReturn(3L);
        when(feedRepository.countByEnabledTrue()).thenReturn(2L);
        when(articleRepository.count()).thenReturn(10L);
        when(articleRepository.countByFetchedAtGreaterThanEqual(Mockito.any(OffsetDateTime.class))).thenReturn(6L);
        when(stateRepository.countByUserKeyAndReadTrue("default")).thenReturn(4L);
        when(stateRepository.countByUserKeyAndArchivedTrue("default")).thenReturn(1L);
        when(stateRepository.countByUserKeyAndFavoriteTrue("default")).thenReturn(2L);
        when(stateRepository.countByUserKeyAndReadLaterTrue("default")).thenReturn(3L);
        when(fetchLogRepository.countByStatus(FetchLogStatus.FAILED)).thenReturn(1L);

        var response = dashboardService.getSummary();

        assertThat(response.feedCount()).isEqualTo(3L);
        assertThat(response.enabledFeedCount()).isEqualTo(2L);
        assertThat(response.articleCount()).isEqualTo(10L);
        assertThat(response.todayNewArticleCount()).isEqualTo(6L);
        assertThat(response.unreadCount()).isEqualTo(5L);
        assertThat(response.failedFetchLogCount()).isEqualTo(1L);
    }

    /**
     * 验证实时信息流使用按入库时间排序的最近文章查询。
     */
    @Test
    void shouldQueryRecentArticlesByFetchedTime() {
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(articleRepository.findRecentlyFetchedArticles(any(Pageable.class))).thenReturn(new PageImpl<>(java.util.List.of()));

        var response = dashboardService.getRecentArticles(4);

        assertThat(response).isEmpty();
        verify(articleRepository).findRecentlyFetchedArticles(pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(4);
    }
}
