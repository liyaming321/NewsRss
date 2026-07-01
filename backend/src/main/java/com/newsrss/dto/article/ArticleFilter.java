package com.newsrss.dto.article;

import java.util.Locale;

/**
 * 文章列表筛选类型。
 */
public enum ArticleFilter {
    ALL,
    UNREAD,
    FAVORITE,
    READ_LATER,
    TODAY;

    /**
     * 将前端筛选参数转换为服务端枚举。
     *
     * @param value 前端传入的筛选值
     * @return 文章筛选类型
     */
    public static ArticleFilter from(String value) {
        if (value == null || value.isBlank()) {
            return ALL;
        }
        return switch (value.strip().toLowerCase(Locale.ROOT)) {
            case "unread" -> UNREAD;
            case "favorite" -> FAVORITE;
            case "readlater", "read_later", "read-later" -> READ_LATER;
            case "today" -> TODAY;
            default -> ALL;
        };
    }
}
