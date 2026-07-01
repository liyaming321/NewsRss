package com.newsrss.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/**
 * 文章标签关联实体，保存文章和标签之间的多对多关系。
 */
@Entity
@Table(name = "rss_article_tag")
public class RssArticleTag {

    @EmbeddedId
    private RssArticleTagId id;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    /**
     * 供 JPA 创建文章标签关联实体时使用。
     */
    protected RssArticleTag() {
    }

    /**
     * 创建文章标签关联实体。
     *
     * @param id 文章标签复合主键
     */
    public RssArticleTag(RssArticleTagId id) {
        this.id = id;
    }
}
