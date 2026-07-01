package com.newsrss.controller;

import com.newsrss.dto.article.ArticleListItemResponse;
import com.newsrss.dto.common.ApiResponse;
import com.newsrss.dto.dashboard.DashboardSummaryResponse;
import com.newsrss.dto.dashboard.FeedHealthResponse;
import com.newsrss.service.dashboard.DashboardService;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 驾驶舱接口。
 */
@RestController
@Profile("db")
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * 查询驾驶舱摘要。
     *
     * @return 驾驶舱摘要
     */
    @GetMapping("/summary")
    public ApiResponse<DashboardSummaryResponse> getSummary() {
        return ApiResponse.ok(dashboardService.getSummary());
    }

    /**
     * 查询订阅源健康状态统计。
     *
     * @return 健康状态统计
     */
    @GetMapping("/feed-health")
    public ApiResponse<FeedHealthResponse> getFeedHealth() {
        return ApiResponse.ok(dashboardService.getFeedHealth());
    }

    /**
     * 查询最近文章。
     *
     * @param limit 返回条数
     * @return 最近文章
     */
    @GetMapping("/recent-articles")
    public ApiResponse<List<ArticleListItemResponse>> getRecentArticles(
            @RequestParam(defaultValue = "8") int limit) {
        return ApiResponse.ok(dashboardService.getRecentArticles(limit));
    }
}
