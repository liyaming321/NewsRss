package com.newsrss.service.article;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newsrss.domain.entity.RssArticle;
import com.newsrss.domain.entity.RssArticleUserState;
import com.newsrss.domain.entity.RssFeed;
import com.newsrss.dto.article.ArticleFlagRequest;
import com.newsrss.dto.article.ArticleFilter;
import com.newsrss.repository.RssArticleRepository;
import com.newsrss.repository.RssArticleUserStateRepository;
import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

class ArticleServiceTest {

    private final RssArticleRepository articleRepository = Mockito.mock(RssArticleRepository.class);
    private final RssArticleUserStateRepository stateRepository = Mockito.mock(RssArticleUserStateRepository.class);
    private final ArticleService articleService = new ArticleService(articleRepository, stateRepository);
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 验证更新收藏状态时会创建默认用户状态并返回最新状态。
     */
    @Test
    void shouldCreateStateWhenUpdatingFavorite() throws Exception {
        RssFeed feed = RssFeed.create("Feed", "https://example.com/rss.xml", 60, now());
        setId(feed, 1L);
        RssArticle article = RssArticle.create(
                feed,
                "guid-1",
                "https://example.com/a",
                "标题",
                "摘要",
                "<p>正文</p>",
                "作者",
                now(),
                null,
                "fingerprint",
                objectMapper.createObjectNode(),
                objectMapper.createObjectNode(),
                objectMapper.createObjectNode(),
                1,
                20,
                now());
        setId(article, 2L);
        when(articleRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(article));
        when(stateRepository.findByArticleIdAndUserKey(2L, "default")).thenReturn(Optional.empty());
        when(stateRepository.save(any(RssArticleUserState.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = articleService.updateFavorite(2L, new ArticleFlagRequest(true));

        assertThat(response.favorite()).isTrue();
        assertThat(response.favoritedAt()).isNotNull();
        assertThat(response.read()).isFalse();
    }

    /**
     * 验证空关键词会转换为空字符串，避免 PostgreSQL 将 null 搜索参数推断成 bytea。
     */
    @Test
    void shouldUseEmptyKeywordWhenListingWithoutSearchText() {
        ArgumentCaptor<String> keywordCaptor = ArgumentCaptor.forClass(String.class);
        when(articleRepository.findArticles(
                eq(null),
                eq("default"),
                eq(ArticleFilter.FAVORITE.name()),
                keywordCaptor.capture(),
                any(OffsetDateTime.class),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        articleService.listArticles(null, "favorite", "   ", 0, 40);

        assertThat(keywordCaptor.getValue()).isEmpty();
    }

    /**
     * 验证文章统计会把筛选条件传给订阅源分组统计。
     */
    @Test
    void shouldPassFilterAndKeywordToFeedStats() {
        ArgumentCaptor<String> filterCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keywordCaptor = ArgumentCaptor.forClass(String.class);
        when(articleRepository.countArticlesGroupByFeed(
                eq("default"),
                filterCaptor.capture(),
                keywordCaptor.capture(),
                any(OffsetDateTime.class)))
                .thenReturn(Collections.singletonList(new Object[] {1L, "Feed", 2L}));
        when(articleRepository.countArticles(any(), eq("default"), anyString(), eq("news"), any(OffsetDateTime.class)))
                .thenReturn(2L);

        var response = articleService.getArticleStats(null, "favorite", " News ");

        assertThat(filterCaptor.getValue()).isEqualTo(ArticleFilter.FAVORITE.name());
        assertThat(keywordCaptor.getValue()).isEqualTo("news");
        assertThat(response.feedStats()).hasSize(1);
        assertThat(response.favoriteCount()).isEqualTo(2L);
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
     * @param target 实体对象
     * @param id 主键
     */
    private void setId(Object target, Long id) throws Exception {
        Field idField = target.getClass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(target, id);
    }
}
