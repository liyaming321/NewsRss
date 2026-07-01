package com.newsrss.service.fetchlog;

import com.newsrss.common.ResourceNotFoundException;
import com.newsrss.domain.entity.RssFeedFetchLog;
import com.newsrss.domain.enums.FetchLogStatus;
import com.newsrss.dto.common.PageResponse;
import com.newsrss.dto.fetchlog.FetchLogResponse;
import com.newsrss.repository.RssFeedFetchLogRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 抓取日志服务，负责日志列表和详情查询。
 */
@Service
@Profile("db")
public class FetchLogService {

    private final RssFeedFetchLogRepository fetchLogRepository;

    public FetchLogService(RssFeedFetchLogRepository fetchLogRepository) {
        this.fetchLogRepository = fetchLogRepository;
    }

    /**
     * 分页查询抓取日志。
     *
     * @param page 页码
     * @param size 每页条数
     * @return 抓取日志分页
     */
    @Transactional(readOnly = true)
    public PageResponse<FetchLogResponse> listFetchLogs(
            int page,
            int size,
            Long feedId,
            String status,
            OffsetDateTime startedFrom,
            OffsetDateTime startedTo) {
        PageRequest pageRequest = PageRequest.of(
                normalizePage(page),
                normalizeSize(size),
                Sort.by(Sort.Direction.DESC, "startedAt"));
        Page<RssFeedFetchLog> logPage = fetchLogRepository.findAll(
                buildLogSpecification(feedId, parseStatus(status), startedFrom, startedTo),
                pageRequest);
        return new PageResponse<>(
                logPage.stream().map(this::toResponse).toList(),
                logPage.getNumber(),
                logPage.getSize(),
                logPage.getTotalElements(),
                logPage.getTotalPages());
    }

    /**
     * 查询抓取日志详情。
     *
     * @param id 日志主键
     * @return 抓取日志详情
     */
    @Transactional(readOnly = true)
    public FetchLogResponse getFetchLog(Long id) {
        return toResponse(fetchLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("抓取日志不存在：" + id)));
    }

    /**
     * 转换抓取日志响应。
     *
     * @param fetchLog 抓取日志实体
     * @return 抓取日志响应
     */
    public FetchLogResponse toResponse(RssFeedFetchLog fetchLog) {
        return new FetchLogResponse(
                fetchLog.getId(),
                fetchLog.getFeed().getId(),
                fetchLog.getFeed().getFeedName(),
                fetchLog.getStartedAt(),
                fetchLog.getFinishedAt(),
                fetchLog.getStatus().name(),
                fetchLog.getRequestUrl(),
                fetchLog.getHttpStatus(),
                fetchLog.getFetchedCount(),
                fetchLog.getNewCount(),
                fetchLog.getDuplicateCount(),
                fetchLog.getFailedCount(),
                fetchLog.getDurationMs(),
                fetchLog.getErrorMessage(),
                fetchLog.getErrorStack(),
                fetchLog.getRawResponseSample(),
                fetchLog.getCreatedAt());
    }

    /**
     * 归一化页码。
     *
     * @param page 页码
     * @return 合法页码
     */
    private int normalizePage(int page) {
        return Math.max(page, 0);
    }

    /**
     * 归一化每页条数。
     *
     * @param size 每页条数
     * @return 合法每页条数
     */
    private int normalizeSize(int size) {
        return Math.max(1, Math.min(size, 100));
    }

    /**
     * 解析可选抓取状态。
     *
     * @param status 状态文本
     * @return 抓取状态，空值时返回 null
     */
    private FetchLogStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return FetchLogStatus.valueOf(status.strip().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("抓取状态不合法：" + status);
        }
    }

    /**
     * 构建抓取日志动态筛选条件。
     *
     * @param feedId 订阅源主键
     * @param status 抓取状态
     * @param startedFrom 开始时间下限
     * @param startedTo 开始时间上限
     * @return 抓取日志筛选条件
     */
    private Specification<RssFeedFetchLog> buildLogSpecification(
            Long feedId,
            FetchLogStatus status,
            OffsetDateTime startedFrom,
            OffsetDateTime startedTo) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (feedId != null) {
                predicates.add(criteriaBuilder.equal(root.get("feed").get("id"), feedId));
            }
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (startedFrom != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("startedAt"), startedFrom));
            }
            if (startedTo != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("startedAt"), startedTo));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
