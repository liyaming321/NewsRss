package com.newsrss.controller;

import com.newsrss.dto.article.ArticleFlagRequest;
import com.newsrss.dto.article.ArticleListItemResponse;
import com.newsrss.dto.article.ArticleResponse;
import com.newsrss.dto.article.ArticleStateResponse;
import com.newsrss.dto.article.ArticleStatsResponse;
import com.newsrss.dto.common.ApiResponse;
import com.newsrss.dto.common.PageResponse;
import com.newsrss.service.article.ArticleService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文章接口。
 */
@RestController
@Profile("db")
@RequestMapping("/api/articles")
public class ArticleController {

    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    /**
     * 分页查询文章。
     *
     * @param feedId 订阅源主键
     * @param filter 筛选类型
     * @param keyword 搜索关键词
     * @param page 页码
     * @param size 每页条数
     * @return 文章分页响应
     */
    @GetMapping
    public ApiResponse<PageResponse<ArticleListItemResponse>> listArticles(
            @RequestParam(required = false) Long feedId,
            @RequestParam(required = false) String filter,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(articleService.listArticles(feedId, filter, keyword, page, size));
    }

    /**
     * 查询文章统计。
     *
     * @return 文章统计响应
     */
    @GetMapping("/stats")
    public ApiResponse<ArticleStatsResponse> getArticleStats(
            @RequestParam(required = false) Long feedId,
            @RequestParam(required = false) String filter,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(articleService.getArticleStats(feedId, filter, keyword));
    }

    /**
     * 查询文章详情。
     *
     * @param id 文章主键
     * @return 文章详情
     */
    @GetMapping("/{id}")
    public ApiResponse<ArticleResponse> getArticle(@PathVariable Long id) {
        return ApiResponse.ok(articleService.getArticle(id));
    }

    /**
     * 更新已读状态。
     *
     * @param id 文章主键
     * @param request 状态请求
     * @return 更新后的文章状态
     */
    @PatchMapping("/{id}/read-state")
    public ApiResponse<ArticleStateResponse> updateReadState(
            @PathVariable Long id,
            @Valid @RequestBody ArticleFlagRequest request) {
        return ApiResponse.ok(articleService.updateReadState(id, request));
    }

    /**
     * 更新收藏状态。
     *
     * @param id 文章主键
     * @param request 状态请求
     * @return 更新后的文章状态
     */
    @PatchMapping("/{id}/favorite")
    public ApiResponse<ArticleStateResponse> updateFavorite(
            @PathVariable Long id,
            @Valid @RequestBody ArticleFlagRequest request) {
        return ApiResponse.ok(articleService.updateFavorite(id, request));
    }

    /**
     * 更新稍后读状态。
     *
     * @param id 文章主键
     * @param request 状态请求
     * @return 更新后的文章状态
     */
    @PatchMapping("/{id}/read-later")
    public ApiResponse<ArticleStateResponse> updateReadLater(
            @PathVariable Long id,
            @Valid @RequestBody ArticleFlagRequest request) {
        return ApiResponse.ok(articleService.updateReadLater(id, request));
    }

    /**
     * 更新归档状态。
     *
     * @param id 文章主键
     * @param request 状态请求
     * @return 更新后的文章状态
     */
    @PatchMapping("/{id}/archive")
    public ApiResponse<ArticleStateResponse> updateArchive(
            @PathVariable Long id,
            @Valid @RequestBody ArticleFlagRequest request) {
        return ApiResponse.ok(articleService.updateArchive(id, request));
    }
}
