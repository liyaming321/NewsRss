package com.newsrss.repository;

import com.newsrss.domain.entity.SystemDictionary;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 系统字典数据访问接口。
 */
public interface SystemDictionaryRepository extends JpaRepository<SystemDictionary, Long> {

    /**
     * 按字典类型查询字典项。
     *
     * @param dictType 字典类型
     * @return 字典项列表
     */
    List<SystemDictionary> findByDictTypeOrderBySortOrderAscItemLabelAsc(String dictType);

    /**
     * 按字典类型和启用状态查询字典项。
     *
     * @param dictType 字典类型
     * @param enabled 是否启用
     * @return 字典项列表
     */
    List<SystemDictionary> findByDictTypeAndEnabledOrderBySortOrderAscItemLabelAsc(String dictType, boolean enabled);

    /**
     * 按类型和编码查询字典项。
     *
     * @param dictType 字典类型
     * @param itemCode 字典项编码
     * @return 匹配的字典项
     */
    Optional<SystemDictionary> findByDictTypeAndItemCode(String dictType, String itemCode);

    /**
     * 判断同类型下编码是否存在。
     *
     * @param dictType 字典类型
     * @param itemCode 字典项编码
     * @return 存在时返回 true
     */
    boolean existsByDictTypeAndItemCode(String dictType, String itemCode);
}
