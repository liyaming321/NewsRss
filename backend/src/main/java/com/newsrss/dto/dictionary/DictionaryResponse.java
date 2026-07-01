package com.newsrss.dto.dictionary;

import java.time.OffsetDateTime;

/**
 * 系统字典响应。
 *
 * @param id 字典项主键
 * @param dictType 字典类型
 * @param itemCode 字典项编码
 * @param itemLabel 字典项名称
 * @param description 字典项说明
 * @param sortOrder 排序值
 * @param enabled 是否启用
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record DictionaryResponse(
        Long id,
        String dictType,
        String itemCode,
        String itemLabel,
        String description,
        Integer sortOrder,
        boolean enabled,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
