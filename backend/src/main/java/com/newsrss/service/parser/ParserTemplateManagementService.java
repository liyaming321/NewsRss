package com.newsrss.service.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.newsrss.common.ResourceNotFoundException;
import com.newsrss.domain.entity.RssFeed;
import com.newsrss.domain.entity.RssParserTemplate;
import com.newsrss.dto.parser.BindParserTemplateRequest;
import com.newsrss.dto.parser.ParserTemplateFeedBindingResponse;
import com.newsrss.dto.parser.ParserTemplateRequest;
import com.newsrss.dto.parser.ParserTemplateResponse;
import com.newsrss.repository.RssFeedRepository;
import com.newsrss.repository.RssParserTemplateRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 解析模板管理服务，负责模板增删改查和订阅源绑定。
 */
@Service
@Profile("db")
public class ParserTemplateManagementService {

    private final RssParserTemplateRepository templateRepository;
    private final RssFeedRepository feedRepository;
    private final ParserTemplateConfigMapper configMapper;

    public ParserTemplateManagementService(
            RssParserTemplateRepository templateRepository,
            RssFeedRepository feedRepository,
            ParserTemplateConfigMapper configMapper) {
        this.templateRepository = templateRepository;
        this.feedRepository = feedRepository;
        this.configMapper = configMapper;
    }

    /**
     * 查询全部解析模板。
     *
     * @return 解析模板列表
     */
    @Transactional(readOnly = true)
    public List<ParserTemplateResponse> listTemplates() {
        return templateRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 按主键查询解析模板。
     *
     * @param id 模板主键
     * @return 解析模板响应
     */
    @Transactional(readOnly = true)
    public ParserTemplateResponse getTemplate(Long id) {
        return toResponse(findTemplate(id));
    }

    /**
     * 创建解析模板。
     *
     * @param request 保存请求
     * @return 创建后的模板响应
     */
    @Transactional
    public ParserTemplateResponse createTemplate(ParserTemplateRequest request) {
        templateRepository.findByTemplateCode(request.templateCode().strip())
                .ifPresent(template -> {
                    throw new IllegalArgumentException("模板编码已存在：" + template.getTemplateCode());
                });
        OffsetDateTime now = now();
        RssParserTemplate template = RssParserTemplate.create(
                request.templateCode().strip(),
                request.templateName().strip(),
                normalizeBlank(request.description()),
                defaultObject(request.fieldMapping()),
                defaultObject(request.customFieldMapping()),
                defaultArray(request.contentSelectors()),
                defaultArray(request.coverSelectors()),
                defaultArray(request.timeFormats()),
                defaultObject(request.cleanupRules()),
                request.enabled() == null || request.enabled(),
                now);
        return toResponse(templateRepository.save(template));
    }

    /**
     * 更新解析模板。
     *
     * @param id 模板主键
     * @param request 保存请求
     * @return 更新后的模板响应
     */
    @Transactional
    public ParserTemplateResponse updateTemplate(Long id, ParserTemplateRequest request) {
        RssParserTemplate template = findTemplate(id);
        templateRepository.findByTemplateCode(request.templateCode().strip())
                .filter(existingTemplate -> !existingTemplate.getId().equals(id))
                .ifPresent(existingTemplate -> {
                    throw new IllegalArgumentException("模板编码已存在：" + existingTemplate.getTemplateCode());
                });
        template.update(
                request.templateCode().strip(),
                request.templateName().strip(),
                normalizeBlank(request.description()),
                defaultObject(request.fieldMapping()),
                defaultObject(request.customFieldMapping()),
                defaultArray(request.contentSelectors()),
                defaultArray(request.coverSelectors()),
                defaultArray(request.timeFormats()),
                defaultObject(request.cleanupRules()),
                request.enabled() == null || request.enabled(),
                now());
        return toResponse(template);
    }

    /**
     * 删除解析模板。
     *
     * @param id 模板主键
     */
    @Transactional
    public void deleteTemplate(Long id) {
        RssParserTemplate template = findTemplate(id);
        templateRepository.delete(template);
    }

    /**
     * 启用或停用解析模板。
     *
     * @param id 模板主键
     * @param enabled 是否启用
     * @return 更新后的模板响应
     */
    @Transactional
    public ParserTemplateResponse setTemplateEnabled(Long id, boolean enabled) {
        RssParserTemplate template = findTemplate(id);
        template.setEnabled(enabled, now());
        return toResponse(template);
    }

    /**
     * 为订阅源绑定解析模板。
     *
     * @param feedId 订阅源主键
     * @param request 绑定请求
     * @return 绑定结果
     */
    @Transactional
    public ParserTemplateFeedBindingResponse bindFeedTemplate(Long feedId, BindParserTemplateRequest request) {
        RssFeed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new ResourceNotFoundException("订阅源不存在：" + feedId));
        RssParserTemplate template = null;
        if (request.parserTemplateId() != null) {
            template = findTemplate(request.parserTemplateId());
            if (!template.isEnabled()) {
                throw new IllegalArgumentException("解析模板已停用，不能绑定：" + template.getTemplateCode());
            }
        }
        feed.bindParserTemplate(template, now());
        return new ParserTemplateFeedBindingResponse(
                feed.getId(),
                feed.getFeedUrl(),
                template == null ? null : template.getId(),
                template == null ? null : template.getTemplateCode());
    }

    /**
     * 获取模板实体。
     *
     * @param id 模板主键
     * @return 模板实体
     */
    @Transactional(readOnly = true)
    public RssParserTemplate findTemplate(Long id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("解析模板不存在：" + id));
    }

    /**
     * 将模板实体转换为响应。
     *
     * @param template 模板实体
     * @return 模板响应
     */
    public ParserTemplateResponse toResponse(RssParserTemplate template) {
        return new ParserTemplateResponse(
                template.getId(),
                template.getTemplateCode(),
                template.getTemplateName(),
                template.getDescription(),
                template.getFieldMapping(),
                template.getCustomFieldMapping(),
                template.getContentSelectors(),
                template.getCoverSelectors(),
                template.getTimeFormats(),
                template.getCleanupRules(),
                template.isEnabled(),
                template.getCreatedAt(),
                template.getUpdatedAt());
    }

    private JsonNode defaultObject(JsonNode node) {
        return node == null || node.isNull() ? configMapper.emptyObject() : node;
    }

    private JsonNode defaultArray(JsonNode node) {
        return node == null || node.isNull() ? configMapper.emptyArray() : node;
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    private String normalizeBlank(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }
}
