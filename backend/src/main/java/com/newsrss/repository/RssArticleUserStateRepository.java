package com.newsrss.repository;

import com.newsrss.domain.entity.RssArticleUserState;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 文章用户状态数据访问接口。
 */
public interface RssArticleUserStateRepository extends JpaRepository<RssArticleUserState, Long> {

    /**
     * 按文章和用户标识查询阅读状态。
     *
     * @param articleId 文章主键
     * @param userKey 用户标识
     * @return 匹配的文章用户状态
     */
    Optional<RssArticleUserState> findByArticleIdAndUserKey(Long articleId, String userKey);

    /**
     * 统计指定用户未归档且未读文章数量。
     *
     * @param userKey 用户标识
     * @return 未读数量
     */
    long countByUserKeyAndReadFalseAndArchivedFalse(String userKey);

    /**
     * 统计指定用户已读文章数量。
     *
     * @param userKey 用户标识
     * @return 已读数量
     */
    long countByUserKeyAndReadTrue(String userKey);

    /**
     * 统计指定用户归档文章数量。
     *
     * @param userKey 用户标识
     * @return 归档数量
     */
    long countByUserKeyAndArchivedTrue(String userKey);

    /**
     * 统计指定用户收藏文章数量。
     *
     * @param userKey 用户标识
     * @return 收藏数量
     */
    long countByUserKeyAndFavoriteTrue(String userKey);

    /**
     * 统计指定用户稍后读文章数量。
     *
     * @param userKey 用户标识
     * @return 稍后读数量
     */
    long countByUserKeyAndReadLaterTrue(String userKey);
}
