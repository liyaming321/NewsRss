package com.newsrss.dto.article;

import java.time.OffsetDateTime;

/**
 * 文章列表项响应。
 *
 * @param id 文章主键
 * @param feedId 订阅源主键
 * @param feedName 订阅源名称
 * @param title 标题
 * @param articleUrl 文章链接
 * @param summary 摘要
 * @param author 作者
 * @param publishedAt 发布时间
 * @param fetchedAt 抓取时间
 * @param coverImageUrl 封面图
 * @param readingMinutes 预计阅读分钟
 * @param wordCount 字数
 * @param state 阅读状态
 */
public record ArticleListItemResponse(
        Long id,
        Long feedId,
        String feedName,
        String title,
        String articleUrl,
        String summary,
        String author,
        OffsetDateTime publishedAt,
        OffsetDateTime fetchedAt,
        String coverImageUrl,
        Integer readingMinutes,
        Integer wordCount,
        ArticleStateResponse state) {
}
