package com.newsrss.common;

import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import com.newsrss.service.rss.RssFetchException;

/**
 * API 异常处理器，为前端和人工测试提供可读错误响应。
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /**
     * 处理参数校验异常。
     *
     * @param exception 参数校验异常
     * @param request HTTP 请求
     * @return 统一错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        List<String> details = exception.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .toList();
        return buildResponse(HttpStatus.BAD_REQUEST, "请求参数不合法", details, request.getRequestURI());
    }

    /**
     * 处理资源不存在异常。
     *
     * @param exception 资源不存在异常
     * @param request HTTP 请求
     * @return 统一错误响应
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException exception,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), List.of(), request.getRequestURI());
    }

    /**
     * 处理业务参数异常。
     *
     * @param exception 业务参数异常
     * @param request HTTP 请求
     * @return 统一错误响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException exception,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), List.of(), request.getRequestURI());
    }

    /**
     * 处理 RSS 抓取异常。
     *
     * @param exception RSS 抓取异常
     * @param request HTTP 请求
     * @return 统一错误响应
     */
    @ExceptionHandler(RssFetchException.class)
    public ResponseEntity<ApiErrorResponse> handleRssFetchException(
            RssFetchException exception,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_GATEWAY, exception.getMessage(), List.of(), request.getRequestURI());
    }

    /**
     * 处理请求参数类型错误。
     *
     * @param exception 参数类型错误异常
     * @param request HTTP 请求
     * @return 统一错误响应
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatchException(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request) {
        String detail = exception.getName() + ": 参数类型不正确";
        return buildResponse(HttpStatus.BAD_REQUEST, "请求参数不合法", List.of(detail), request.getRequestURI());
    }

    /**
     * 处理必填请求参数缺失。
     *
     * @param exception 缺少参数异常
     * @param request HTTP 请求
     * @return 统一错误响应
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParameterException(
            MissingServletRequestParameterException exception,
            HttpServletRequest request) {
        String detail = exception.getParameterName() + ": 参数不能为空";
        return buildResponse(HttpStatus.BAD_REQUEST, "请求参数不合法", List.of(detail), request.getRequestURI());
    }

    /**
     * 格式化字段校验错误。
     *
     * @param fieldError 字段错误
     * @return 可读错误文本
     */
    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }

    /**
     * 创建统一错误响应实体。
     *
     * @param status HTTP 状态
     * @param message 可读错误信息
     * @param details 错误明细
     * @param path 请求路径
     * @return 错误响应实体
     */
    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status,
            String message,
            List<String> details,
            String path) {
        ApiErrorResponse response = new ApiErrorResponse(
                status.value(),
                message,
                details,
                path,
                OffsetDateTime.now(ZoneOffset.UTC));
        return ResponseEntity.status(status).body(response);
    }
}
