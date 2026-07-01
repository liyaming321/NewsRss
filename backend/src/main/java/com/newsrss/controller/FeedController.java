package com.newsrss.controller;

import com.newsrss.dto.RssFetchResult;
import com.newsrss.dto.common.ApiResponse;
import com.newsrss.dto.common.PageResponse;
import com.newsrss.dto.feed.FeedBatchRefreshRequest;
import com.newsrss.dto.feed.FeedBatchRefreshResponse;
import com.newsrss.dto.feed.FeedDetectRequest;
import com.newsrss.dto.feed.FeedDetectResponse;
import com.newsrss.dto.feed.FeedRequest;
import com.newsrss.dto.feed.FeedResponse;
import com.newsrss.dto.feed.FeedScheduleRunResponse;
import com.newsrss.service.feed.FeedService;
import com.newsrss.service.feed.RssFeedScheduleService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订阅源接口。
 */
@RestController
@Profile("db")
@RequestMapping("/api/feeds")
public class FeedController {

    private final FeedService feedService;
    private final RssFeedScheduleService scheduleService;

    public FeedController(FeedService feedService, RssFeedScheduleService scheduleService) {
        this.feedService = feedService;
        this.scheduleService = scheduleService;
    }

    /**
     * 分页查询订阅源。
     *
     * @param page 页码
     * @param size 每页条数
     * @return 订阅源分页响应
     */
    @GetMapping
    public ApiResponse<PageResponse<FeedResponse>> listFeeds(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(feedService.listFeeds(page, size));
    }

    /**
     * 创建订阅源。
     *
     * @param request 保存请求
     * @return 创建后的订阅源
     */
    @PostMapping
    public ResponseEntity<ApiResponse<FeedResponse>> createFeed(@Valid @RequestBody FeedRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("订阅源创建成功", feedService.createFeed(request)));
    }

    /**
     * 更新订阅源。
     *
     * @param id 订阅源主键
     * @param request 保存请求
     * @return 更新后的订阅源
     */
    @PutMapping("/{id}")
    public ApiResponse<FeedResponse> updateFeed(
            @PathVariable Long id,
            @Valid @RequestBody FeedRequest request) {
        return ApiResponse.ok("订阅源更新成功", feedService.updateFeed(id, request));
    }

    /**
     * 删除订阅源。
     *
     * @param id 订阅源主键
     * @return 空响应
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteFeed(@PathVariable Long id) {
        feedService.deleteFeed(id);
        return ApiResponse.ok("订阅源删除成功", null);
    }

    /**
     * 手动刷新订阅源。
     *
     * @param id 订阅源主键
     * @return 抓取结果
     */
    @PostMapping("/{id}/refresh")
    public ApiResponse<RssFetchResult> refreshFeed(@PathVariable Long id) {
        return ApiResponse.ok("订阅源刷新已执行", feedService.refreshFeed(id));
    }

    /**
     * 批量刷新订阅源。
     *
     * @param request 批量刷新请求
     * @return 批量刷新结果
     */
    @PostMapping("/refresh-batch")
    public ApiResponse<FeedBatchRefreshResponse> refreshBatch(@RequestBody(required = false) FeedBatchRefreshRequest request) {
        return ApiResponse.ok("批量刷新已执行", scheduleService.refreshBatch(request == null ? null : request.feedIds()));
    }

    /**
     * 手动触发一次到期源调度。
     *
     * @return 调度执行结果
     */
    @PostMapping("/schedule/run-once")
    public ApiResponse<FeedScheduleRunResponse> runScheduleOnce() {
        return ApiResponse.ok("调度刷新已执行", scheduleService.runDueFeeds());
    }

    /**
     * 探测订阅源元数据。
     *
     * @param request 探测请求
     * @return 探测结果
     */
    @PostMapping("/detect")
    public ApiResponse<FeedDetectResponse> detectFeed(@Valid @RequestBody FeedDetectRequest request) {
        return ApiResponse.ok(feedService.detectFeed(request.feedUrl()));
    }
}
