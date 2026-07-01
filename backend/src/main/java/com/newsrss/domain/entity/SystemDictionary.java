package com.newsrss.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/**
 * 系统字典实体，保存订阅源分类等可维护选项。
 */
@Entity
@Table(name = "system_dictionary")
public class SystemDictionary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dict_type", nullable = false, length = 80)
    private String dictType;

    @Column(name = "item_code", nullable = false, length = 80)
    private String itemCode;

    @Column(name = "item_label", nullable = false, length = 120)
    private String itemLabel;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * 供 JPA 创建字典实体时使用。
     */
    protected SystemDictionary() {
    }

    /**
     * 创建系统字典项。
     *
     * @param dictType 字典类型
     * @param itemCode 字典项编码
     * @param itemLabel 字典项名称
     * @param description 字典项说明
     * @param sortOrder 排序值
     * @param enabled 是否启用
     * @param now 当前业务时间
     * @return 可持久化的字典项
     */
    public static SystemDictionary create(
            String dictType,
            String itemCode,
            String itemLabel,
            String description,
            Integer sortOrder,
            boolean enabled,
            OffsetDateTime now) {
        SystemDictionary dictionary = new SystemDictionary();
        dictionary.dictType = dictType;
        dictionary.itemCode = itemCode;
        dictionary.itemLabel = itemLabel;
        dictionary.description = description;
        dictionary.sortOrder = sortOrder == null ? 100 : sortOrder;
        dictionary.enabled = enabled;
        dictionary.createdAt = now;
        dictionary.updatedAt = now;
        return dictionary;
    }

    /**
     * 更新字典项配置。
     *
     * @param itemCode 字典项编码
     * @param itemLabel 字典项名称
     * @param description 字典项说明
     * @param sortOrder 排序值
     * @param enabled 是否启用
     * @param now 当前业务时间
     */
    public void updateConfig(
            String itemCode,
            String itemLabel,
            String description,
            Integer sortOrder,
            boolean enabled,
            OffsetDateTime now) {
        this.itemCode = itemCode;
        this.itemLabel = itemLabel;
        this.description = description;
        this.sortOrder = sortOrder == null ? 100 : sortOrder;
        this.enabled = enabled;
        this.updatedAt = now;
    }

    /**
     * 获取字典项主键。
     *
     * @return 字典项主键
     */
    public Long getId() {
        return id;
    }

    /**
     * 获取字典类型。
     *
     * @return 字典类型
     */
    public String getDictType() {
        return dictType;
    }

    /**
     * 获取字典项编码。
     *
     * @return 字典项编码
     */
    public String getItemCode() {
        return itemCode;
    }

    /**
     * 获取字典项名称。
     *
     * @return 字典项名称
     */
    public String getItemLabel() {
        return itemLabel;
    }

    /**
     * 获取字典项说明。
     *
     * @return 字典项说明
     */
    public String getDescription() {
        return description;
    }

    /**
     * 获取排序值。
     *
     * @return 排序值
     */
    public Integer getSortOrder() {
        return sortOrder;
    }

    /**
     * 判断字典项是否启用。
     *
     * @return 启用时返回 true
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 获取创建时间。
     *
     * @return 创建时间
     */
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 获取更新时间。
     *
     * @return 更新时间
     */
    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
