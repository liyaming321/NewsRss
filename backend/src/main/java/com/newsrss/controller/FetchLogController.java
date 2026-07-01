package com.newsrss.controller;

import com.newsrss.dto.common.ApiResponse;
import com.newsrss.dto.common.PageResponse;
import com.newsrss.dto.fetchlog.FetchLogResponse;
import com.newsrss.service.fetchlog.FetchLogService;
import java.time.OffsetDateTime;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 抓取日志接口。
 */
@RestController
@Profile("db")
@RequestMapping("/api/fetch-logs")
public class FetchLogController {

    private final FetchLogService fetchLogService;

    public FetchLogController(FetchLogService fetchLogService) {
        this.fetchLogService = fetchLogService;
    }

    /**
     * 分页查询抓取日志。
     *
     * @param page 页码
     * @param size 每页条数
     * @param feedId 订阅源主键
     * @param status 抓取状态
     * @param startedFrom 开始时间下限
     * @param startedTo 开始时间上限
     * @return 抓取日志分页
     */
    @GetMapping
    public ApiResponse<PageResponse<FetchLogResponse>> listFetchLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long feedId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startedTo) {
        return ApiResponse.ok(fetchLogService.listFetchLogs(page, size, feedId, status, startedFrom, startedTo));
    }

    /**
     * 查询抓取日志详情。
     *
     * @param id 日志主键
     * @return 抓取日志详情
     */
    @GetMapping("/{id}")
    public ApiResponse<FetchLogResponse> getFetchLog(@PathVariable Long id) {
        return ApiResponse.ok(fetchLogService.getFetchLog(id));
    }
}
