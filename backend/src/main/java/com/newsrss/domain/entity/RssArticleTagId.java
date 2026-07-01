package com.newsrss.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * 文章标签关联主键，由文章主键和标签主键共同组成。
 */
@Embeddable
public class RssArticleTagId implements Serializable {

    @Column(name = "article_id", nullable = false)
    private Long articleId;

    @Column(name = "tag_id", nullable = false)
    private Long tagId;

    /**
     * 供 JPA 创建文章标签关联主键时使用。
     */
    protected RssArticleTagId() {
    }

    /**
     * 创建文章标签关联主键。
     *
     * @param articleId 文章主键
     * @param tagId 标签主键
     */
    public RssArticleTagId(Long articleId, Long tagId) {
        this.articleId = articleId;
        this.tagId = tagId;
    }

    /**
     * 判断两个关联主键是否指向同一篇文章和同一个标签。
     *
     * @param object 待比较对象
     * @return 如果文章主键和标签主键都相同则返回 true
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof RssArticleTagId that)) {
            return false;
        }
        return Objects.equals(articleId, that.articleId) && Objects.equals(tagId, that.tagId);
    }

    /**
     * 生成复合主键哈希值。
     *
     * @return 由文章主键和标签主键生成的哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(articleId, tagId);
    }
}
