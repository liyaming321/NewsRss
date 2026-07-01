package com.newsrss.repository;

import com.newsrss.domain.entity.RssArticleTag;
import com.newsrss.domain.entity.RssArticleTagId;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 文章标签关联数据访问接口。
 */
public interface RssArticleTagRepository extends JpaRepository<RssArticleTag, RssArticleTagId> {
}
