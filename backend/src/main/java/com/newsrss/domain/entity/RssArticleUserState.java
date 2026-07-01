package com.newsrss.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/**
 * 文章用户状态实体，保存已读、收藏、稍后读和归档等个人阅读状态。
 */
@Entity
@Table(name = "rss_article_user_state")
public class RssArticleUserState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "article_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_rss_article_user_state_article"))
    private RssArticle article;

    @Column(name = "user_key", nullable = false, length = 80)
    private String userKey = "default";

    @Column(name = "read", nullable = false)
    private boolean read;

    @Column(name = "favorite", nullable = false)
    private boolean favorite;

    @Column(name = "read_later", nullable = false)
    private boolean readLater;

    @Column(name = "archived", nullable = false)
    private boolean archived;

    @Column(name = "read_at")
    private OffsetDateTime readAt;

    @Column(name = "favorited_at")
    private OffsetDateTime favoritedAt;

    @Column(name = "read_later_at")
    private OffsetDateTime readLaterAt;

    @Column(name = "archived_at")
    private OffsetDateTime archivedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * 供 JPA 创建文章用户状态实体时使用。
     */
    protected RssArticleUserState() {
    }

    /**
     * 创建文章用户状态。
     *
     * @param article 文章实体
     * @param userKey 用户标识
     * @param now 当前业务时间
     * @return 可持久化的文章用户状态
     */
    public static RssArticleUserState create(RssArticle article, String userKey, OffsetDateTime now) {
        RssArticleUserState state = new RssArticleUserState();
        state.article = article;
        state.userKey = userKey;
        state.createdAt = now;
        state.updatedAt = now;
        return state;
    }

    /**
     * 设置已读状态。
     *
     * @param read 是否已读
     * @param now 当前业务时间
     */
    public void setRead(boolean read, OffsetDateTime now) {
        this.read = read;
        this.readAt = read ? now : null;
        this.updatedAt = now;
    }

    /**
     * 设置收藏状态。
     *
     * @param favorite 是否收藏
     * @param now 当前业务时间
     */
    public void setFavorite(boolean favorite, OffsetDateTime now) {
        this.favorite = favorite;
        this.favoritedAt = favorite ? now : null;
        this.updatedAt = now;
    }

    /**
     * 设置稍后读状态。
     *
     * @param readLater 是否稍后读
     * @param now 当前业务时间
     */
    public void setReadLater(boolean readLater, OffsetDateTime now) {
        this.readLater = readLater;
        this.readLaterAt = readLater ? now : null;
        this.updatedAt = now;
    }

    /**
     * 设置归档状态。
     *
     * @param archived 是否归档
     * @param now 当前业务时间
     */
    public void setArchived(boolean archived, OffsetDateTime now) {
        this.archived = archived;
        this.archivedAt = archived ? now : null;
        this.updatedAt = now;
    }

    /**
     * 获取状态主键。
     *
     * @return 状态主键
     */
    public Long getId() {
        return id;
    }

    /**
     * 获取文章实体。
     *
     * @return 文章实体
     */
    public RssArticle getArticle() {
        return article;
    }

    /**
     * 获取用户标识。
     *
     * @return 用户标识
     */
    public String getUserKey() {
        return userKey;
    }

    /**
     * 判断是否已读。
     *
     * @return 已读时返回 true
     */
    public boolean isRead() {
        return read;
    }

    /**
     * 判断是否收藏。
     *
     * @return 收藏时返回 true
     */
    public boolean isFavorite() {
        return favorite;
    }

    /**
     * 判断是否稍后读。
     *
     * @return 稍后读时返回 true
     */
    public boolean isReadLater() {
        return readLater;
    }

    /**
     * 判断是否归档。
     *
     * @return 归档时返回 true
     */
    public boolean isArchived() {
        return archived;
    }

    /**
     * 获取已读时间。
     *
     * @return 已读时间
     */
    public OffsetDateTime getReadAt() {
        return readAt;
    }

    /**
     * 获取收藏时间。
     *
     * @return 收藏时间
     */
    public OffsetDateTime getFavoritedAt() {
        return favoritedAt;
    }

    /**
     * 获取稍后读时间。
     *
     * @return 稍后读时间
     */
    public OffsetDateTime getReadLaterAt() {
        return readLaterAt;
    }

    /**
     * 获取归档时间。
     *
     * @return 归档时间
     */
    public OffsetDateTime getArchivedAt() {
        return archivedAt;
    }
}
