package com.newsrss.domain.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * RSS 解析模板实体，保存不同订阅源的字段映射、候选字段和清洗规则。
 */
@Entity
@Table(name = "rss_parser_template")
public class RssParserTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_code", nullable = false, length = 80)
    private String templateCode;

    @Column(name = "template_name", nullable = false, length = 120)
    private String templateName;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "field_mapping", nullable = false, columnDefinition = "jsonb")
    private JsonNode fieldMapping;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "custom_field_mapping", nullable = false, columnDefinition = "jsonb")
    private JsonNode customFieldMapping;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content_selectors", nullable = false, columnDefinition = "jsonb")
    private JsonNode contentSelectors;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cover_selectors", nullable = false, columnDefinition = "jsonb")
    private JsonNode coverSelectors;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "time_formats", nullable = false, columnDefinition = "jsonb")
    private JsonNode timeFormats;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cleanup_rules", nullable = false, columnDefinition = "jsonb")
    private JsonNode cleanupRules;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * 供 JPA 创建解析模板实体时使用。
     */
    protected RssParserTemplate() {
    }

    /**
     * 创建解析模板。
     *
     * @param templateCode 模板编码
     * @param templateName 模板名称
     * @param description 模板说明
     * @param fieldMapping 字段映射配置
     * @param customFieldMapping 自定义字段映射配置
     * @param contentSelectors 正文候选字段列表
     * @param coverSelectors 封面候选字段列表
     * @param timeFormats 自定义时间格式列表
     * @param cleanupRules 清洗规则
     * @param enabled 是否启用
     * @param now 当前业务时间
     * @return 可持久化的解析模板实体
     */
    public static RssParserTemplate create(
            String templateCode,
            String templateName,
            String description,
            JsonNode fieldMapping,
            JsonNode customFieldMapping,
            JsonNode contentSelectors,
            JsonNode coverSelectors,
            JsonNode timeFormats,
            JsonNode cleanupRules,
            boolean enabled,
            OffsetDateTime now) {
        RssParserTemplate template = new RssParserTemplate();
        template.templateCode = templateCode;
        template.templateName = templateName;
        template.description = description;
        template.fieldMapping = fieldMapping;
        template.customFieldMapping = customFieldMapping;
        template.contentSelectors = contentSelectors;
        template.coverSelectors = coverSelectors;
        template.timeFormats = timeFormats;
        template.cleanupRules = cleanupRules;
        template.enabled = enabled;
        template.createdAt = now;
        template.updatedAt = now;
        return template;
    }

    /**
     * 更新解析模板配置。
     *
     * @param templateCode 模板编码
     * @param templateName 模板名称
     * @param description 模板说明
     * @param fieldMapping 字段映射配置
     * @param customFieldMapping 自定义字段映射配置
     * @param contentSelectors 正文候选字段列表
     * @param coverSelectors 封面候选字段列表
     * @param timeFormats 自定义时间格式列表
     * @param cleanupRules 清洗规则
     * @param enabled 是否启用
     * @param now 当前业务时间
     */
    public void update(
            String templateCode,
            String templateName,
            String description,
            JsonNode fieldMapping,
            JsonNode customFieldMapping,
            JsonNode contentSelectors,
            JsonNode coverSelectors,
            JsonNode timeFormats,
            JsonNode cleanupRules,
            boolean enabled,
            OffsetDateTime now) {
        this.templateCode = templateCode;
        this.templateName = templateName;
        this.description = description;
        this.fieldMapping = fieldMapping;
        this.customFieldMapping = customFieldMapping;
        this.contentSelectors = contentSelectors;
        this.coverSelectors = coverSelectors;
        this.timeFormats = timeFormats;
        this.cleanupRules = cleanupRules;
        this.enabled = enabled;
        this.updatedAt = now;
    }

    /**
     * 设置模板启用状态。
     *
     * @param enabled 是否启用
     * @param now 当前业务时间
     */
    public void setEnabled(boolean enabled, OffsetDateTime now) {
        this.enabled = enabled;
        this.updatedAt = now;
    }

    /**
     * 获取模板主键。
     *
     * @return 模板主键
     */
    public Long getId() {
        return id;
    }

    /**
     * 获取模板编码。
     *
     * @return 模板编码
     */
    public String getTemplateCode() {
        return templateCode;
    }

    /**
     * 获取模板名称。
     *
     * @return 模板名称
     */
    public String getTemplateName() {
        return templateName;
    }

    /**
     * 获取模板说明。
     *
     * @return 模板说明
     */
    public String getDescription() {
        return description;
    }

    /**
     * 获取字段映射配置。
     *
     * @return 字段映射配置
     */
    public JsonNode getFieldMapping() {
        return fieldMapping;
    }

    /**
     * 获取自定义字段映射配置。
     *
     * @return 自定义字段映射配置
     */
    public JsonNode getCustomFieldMapping() {
        return customFieldMapping;
    }

    /**
     * 获取正文候选字段列表。
     *
     * @return 正文候选字段列表
     */
    public JsonNode getContentSelectors() {
        return contentSelectors;
    }

    /**
     * 获取封面候选字段列表。
     *
     * @return 封面候选字段列表
     */
    public JsonNode getCoverSelectors() {
        return coverSelectors;
    }

    /**
     * 获取自定义时间格式列表。
     *
     * @return 自定义时间格式列表
     */
    public JsonNode getTimeFormats() {
        return timeFormats;
    }

    /**
     * 获取清洗规则。
     *
     * @return 清洗规则
     */
    public JsonNode getCleanupRules() {
        return cleanupRules;
    }

    /**
     * 判断模板是否启用。
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
