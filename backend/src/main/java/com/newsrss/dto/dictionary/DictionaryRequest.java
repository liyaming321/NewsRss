package com.newsrss.dto.dictionary;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 系统字典保存请求。
 *
 * @param itemCode 字典项编码
 * @param itemLabel 字典项名称
 * @param description 字典项说明
 * @param sortOrder 排序值
 * @param enabled 是否启用
 */
public record DictionaryRequest(
        @NotBlank(message = "字典项编码不能为空")
        @Size(max = 80, message = "字典项编码不能超过 80 个字符")
        String itemCode,
        @NotBlank(message = "字典项名称不能为空")
        @Size(max = 120, message = "字典项名称不能超过 120 个字符")
        String itemLabel,
        String description,
        @Min(value = 0, message = "排序值不能小于 0")
        Integer sortOrder,
        Boolean enabled) {
}
