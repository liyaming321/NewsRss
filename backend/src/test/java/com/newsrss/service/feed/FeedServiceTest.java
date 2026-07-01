package com.newsrss.service.feed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.newsrss.domain.entity.RssFeed;
import com.newsrss.dto.feed.FeedRequest;
import com.newsrss.repository.RssArticleRepository;
import com.newsrss.repository.RssFeedRepository;
import com.newsrss.repository.RssParserTemplateRepository;
import com.newsrss.service.dictionary.DictionaryService;
import com.newsrss.service.rss.RssFeedFetchService;
import com.newsrss.service.rss.RssFeedParser;
import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class FeedServiceTest {

    private final RssFeedRepository feedRepository = Mockito.mock(RssFeedRepository.class);
    private final RssParserTemplateRepository templateRepository = Mockito.mock(RssParserTemplateRepository.class);
    private final RssArticleRepository articleRepository = Mockito.mock(RssArticleRepository.class);
    private final RssFeedParser feedParser = Mockito.mock(RssFeedParser.class);
    private final RssFeedFetchService fetchService = Mockito.mock(RssFeedFetchService.class);
    private final DictionaryService dictionaryService = Mockito.mock(DictionaryService.class);
    private final FeedService feedService = new FeedService(
            feedRepository,
            templateRepository,
            articleRepository,
            feedParser,
            fetchService,
            dictionaryService);

    /**
     * 验证创建订阅源时会保存基础配置并返回响应。
     */
    @Test
    void shouldCreateFeed() throws Exception {
        when(feedRepository.findByFeedUrl("https://example.com/rss.xml")).thenReturn(Optional.empty());
        when(feedRepository.save(any(RssFeed.class))).thenAnswer(invocation -> {
            RssFeed feed = invocation.getArgument(0);
            setId(feed, 10L);
            return feed;
        });
        when(articleRepository.countByFeedId(10L)).thenReturn(0L);
        FeedRequest request = new FeedRequest(
                "Example",
                "https://example.com/rss.xml",
                "Tech",
                null,
                null,
                30,
                true);

        var response = feedService.createFeed(request);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.feedName()).isEqualTo("Example");
        assertThat(response.feedUrl()).isEqualTo("https://example.com/rss.xml");
        assertThat(response.fetchIntervalMinutes()).isEqualTo(30);
        assertThat(response.enabled()).isTrue();
    }

    /**
     * 验证重复订阅地址会给出可读异常。
     */
    @Test
    void shouldRejectDuplicateFeedUrl() {
        RssFeed existingFeed = RssFeed.create(
                "Existing",
                "https://example.com/rss.xml",
                60,
                OffsetDateTime.now(ZoneOffset.UTC));
        when(feedRepository.findByFeedUrl("https://example.com/rss.xml")).thenReturn(Optional.of(existingFeed));
        FeedRequest request = new FeedRequest(
                "Example",
                "https://example.com/rss.xml",
                null,
                null,
                null,
                60,
                true);

        assertThatThrownBy(() -> feedService.createFeed(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("订阅源已存在");
    }

    /**
     * 验证创建订阅源时会校验分类字典。
     */
    @Test
    void shouldValidateFeedCategoryWhenCreatingFeed() {
        when(feedRepository.findByFeedUrl("https://example.com/rss.xml")).thenReturn(Optional.empty());
        Mockito.doThrow(new IllegalArgumentException("订阅源分类不存在，请先在设置中新增：Unknown"))
                .when(dictionaryService)
                .validateEnabledFeedCategory("Unknown");
        FeedRequest request = new FeedRequest(
                "Example",
                "https://example.com/rss.xml",
                "Unknown",
                null,
                null,
                60,
                true);

        assertThatThrownBy(() -> feedService.createFeed(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("订阅源分类不存在");
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
