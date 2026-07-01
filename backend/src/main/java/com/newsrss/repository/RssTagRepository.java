package com.newsrss.repository;

import com.newsrss.domain.entity.RssTag;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 文章标签数据访问接口。
 */
public interface RssTagRepository extends JpaRepository<RssTag, Long> {

    /**
     * 按标签标识查询标签。
     *
     * @param slug 标签 URL 友好标识
     * @return 匹配的标签
     */
    Optional<RssTag> findBySlug(String slug);
}
