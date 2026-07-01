package com.newsrss.repository;

import com.newsrss.domain.entity.RssParserTemplate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * RSS 解析模板数据访问接口。
 */
public interface RssParserTemplateRepository extends JpaRepository<RssParserTemplate, Long> {

    /**
     * 按模板编码查询解析模板。
     *
     * @param templateCode 模板编码
     * @return 匹配的解析模板
     */
    Optional<RssParserTemplate> findByTemplateCode(String templateCode);
}
