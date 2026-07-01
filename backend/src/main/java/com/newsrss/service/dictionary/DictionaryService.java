package com.newsrss.service.dictionary;

import com.newsrss.common.ResourceNotFoundException;
import com.newsrss.domain.entity.SystemDictionary;
import com.newsrss.dto.dictionary.DictionaryRequest;
import com.newsrss.dto.dictionary.DictionaryResponse;
import com.newsrss.repository.SystemDictionaryRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 系统字典服务，负责订阅源分类等可配置选项的维护。
 */
@Service
@Profile("db")
public class DictionaryService {

    public static final String FEED_CATEGORY = "FEED_CATEGORY";

    private final SystemDictionaryRepository dictionaryRepository;

    public DictionaryService(SystemDictionaryRepository dictionaryRepository) {
        this.dictionaryRepository = dictionaryRepository;
    }

    /**
     * 查询指定类型字典项。
     *
     * @param dictType 字典类型
     * @param enabled 是否只查询启用项
     * @return 字典项列表
     */
    @Transactional(readOnly = true)
    public List<DictionaryResponse> listItems(String dictType, Boolean enabled) {
        String normalizedType = normalizeDictType(dictType);
        List<SystemDictionary> items = enabled == null
                ? dictionaryRepository.findByDictTypeOrderBySortOrderAscItemLabelAsc(normalizedType)
                : dictionaryRepository.findByDictTypeAndEnabledOrderBySortOrderAscItemLabelAsc(normalizedType, enabled);
        return items.stream().map(this::toResponse).toList();
    }

    /**
     * 创建字典项。
     *
     * @param dictType 字典类型
     * @param request 保存请求
     * @return 创建后的字典项
     */
    @Transactional
    public DictionaryResponse createItem(String dictType, DictionaryRequest request) {
        String normalizedType = normalizeDictType(dictType);
        String itemCode = normalizeRequired(request.itemCode(), "字典项编码不能为空");
        if (dictionaryRepository.existsByDictTypeAndItemCode(normalizedType, itemCode)) {
            throw new IllegalArgumentException("字典项编码已存在：" + itemCode);
        }
        OffsetDateTime now = now();
        SystemDictionary dictionary = SystemDictionary.create(
                normalizedType,
                itemCode,
                normalizeRequired(request.itemLabel(), "字典项名称不能为空"),
                normalizeBlank(request.description()),
                normalizeSortOrder(request.sortOrder()),
                request.enabled() == null || request.enabled(),
                now);
        return toResponse(dictionaryRepository.save(dictionary));
    }

    /**
     * 更新字典项。
     *
     * @param id 字典项主键
     * @param request 保存请求
     * @return 更新后的字典项
     */
    @Transactional
    public DictionaryResponse updateItem(Long id, DictionaryRequest request) {
        SystemDictionary dictionary = findItem(id);
        String itemCode = normalizeRequired(request.itemCode(), "字典项编码不能为空");
        dictionaryRepository.findByDictTypeAndItemCode(dictionary.getDictType(), itemCode)
                .filter(existingItem -> !existingItem.getId().equals(id))
                .ifPresent(existingItem -> {
                    throw new IllegalArgumentException("字典项编码已存在：" + itemCode);
                });
        dictionary.updateConfig(
                itemCode,
                normalizeRequired(request.itemLabel(), "字典项名称不能为空"),
                normalizeBlank(request.description()),
                normalizeSortOrder(request.sortOrder()),
                request.enabled() == null || request.enabled(),
                now());
        return toResponse(dictionary);
    }

    /**
     * 删除字典项。
     *
     * @param id 字典项主键
     */
    @Transactional
    public void deleteItem(Long id) {
        dictionaryRepository.delete(findItem(id));
    }

    /**
     * 校验订阅源分类是否存在且启用。
     *
     * @param category 分类编码
     */
    @Transactional(readOnly = true)
    public void validateEnabledFeedCategory(String category) {
        if (category == null || category.isBlank()) {
            return;
        }
        String normalizedCategory = category.strip();
        SystemDictionary dictionary = dictionaryRepository.findByDictTypeAndItemCode(FEED_CATEGORY, normalizedCategory)
                .orElseThrow(() -> new IllegalArgumentException("订阅源分类不存在，请先在设置中新增：" + normalizedCategory));
        if (!dictionary.isEnabled()) {
            throw new IllegalArgumentException("订阅源分类已停用，请先在设置中启用：" + normalizedCategory);
        }
    }

    /**
     * 查询字典实体。
     *
     * @param id 字典项主键
     * @return 字典实体
     */
    private SystemDictionary findItem(Long id) {
        return dictionaryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("字典项不存在：" + id));
    }

    /**
     * 转换字典响应。
     *
     * @param dictionary 字典实体
     * @return 字典响应
     */
    private DictionaryResponse toResponse(SystemDictionary dictionary) {
        return new DictionaryResponse(
                dictionary.getId(),
                dictionary.getDictType(),
                dictionary.getItemCode(),
                dictionary.getItemLabel(),
                dictionary.getDescription(),
                dictionary.getSortOrder(),
                dictionary.isEnabled(),
                dictionary.getCreatedAt(),
                dictionary.getUpdatedAt());
    }

    /**
     * 归一化字典类型。
     *
     * @param dictType 原始字典类型
     * @return 合法字典类型
     */
    private String normalizeDictType(String dictType) {
        String normalizedType = normalizeRequired(dictType, "字典类型不能为空").toUpperCase(Locale.ROOT);
        if (!FEED_CATEGORY.equals(normalizedType)) {
            throw new IllegalArgumentException("不支持的字典类型：" + normalizedType);
        }
        return normalizedType;
    }

    /**
     * 归一化必填字符串。
     *
     * @param value 原始字符串
     * @param message 缺失提示
     * @return 去除首尾空白后的字符串
     */
    private String normalizeRequired(String value, String message) {
        String normalizedValue = normalizeBlank(value);
        if (normalizedValue == null) {
            throw new IllegalArgumentException(message);
        }
        return normalizedValue;
    }

    /**
     * 将空白字符串统一归一为空值。
     *
     * @param value 原始字符串
     * @return 去除首尾空白后的字符串，空白时返回 null
     */
    private String normalizeBlank(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }

    /**
     * 归一化排序值。
     *
     * @param sortOrder 原始排序值
     * @return 合法排序值
     */
    private Integer normalizeSortOrder(Integer sortOrder) {
        if (sortOrder == null) {
            return 100;
        }
        return Math.max(0, sortOrder);
    }

    /**
     * 获取当前 UTC 时间。
     *
     * @return 当前 UTC 时间
     */
    private OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }
}
