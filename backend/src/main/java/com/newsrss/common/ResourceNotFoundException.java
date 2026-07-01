package com.newsrss.common;

/**
 * 资源不存在异常。
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * 创建资源不存在异常。
     *
     * @param message 可读错误信息
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
