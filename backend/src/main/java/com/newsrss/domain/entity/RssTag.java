package com.newsrss.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/**
 * RSS 文章标签实体，用于对文章进行人工或规则化分类。
 */
@Entity
@Table(name = "rss_tag")
public class RssTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tag_name", nullable = false, length = 80)
    private String tagName;

    @Column(name = "slug", nullable = false, length = 100)
    private String slug;

    @Column(name = "color", length = 32)
    private String color;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * 供 JPA 创建标签实体时使用。
     */
    protected RssTag() {
    }
}
