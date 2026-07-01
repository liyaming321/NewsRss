package com.newsrss.controller;

import com.newsrss.dto.parser.BindParserTemplateRequest;
import com.newsrss.dto.common.ApiResponse;
import com.newsrss.dto.parser.ParserTemplateFeedBindingResponse;
import com.newsrss.dto.parser.ParserTemplateGenerateRequest;
import com.newsrss.dto.parser.ParserTemplateGenerateResponse;
import com.newsrss.dto.parser.ParserTemplatePreviewRequest;
import com.newsrss.dto.parser.ParserTemplatePreviewResponse;
import com.newsrss.dto.parser.ParserTemplateRequest;
import com.newsrss.dto.parser.ParserTemplateResponse;
import com.newsrss.service.parser.ParserTemplateManagementService;
import com.newsrss.service.parser.ParserTemplateGenerationService;
import com.newsrss.service.parser.ParserTemplatePreviewService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 解析模板接口，提供 M3 阶段的模板管理、绑定和预览能力。
 */
@RestController
@Profile("db")
@RequestMapping("/api/parser-templates")
public class ParserTemplateController {

    private final ParserTemplateManagementService managementService;
    private final ParserTemplatePreviewService previewService;
    private final ParserTemplateGenerationService generationService;

    public ParserTemplateController(
            ParserTemplateManagementService managementService,
            ParserTemplatePreviewService previewService,
            ParserTemplateGenerationService generationService) {
        this.managementService = managementService;
        this.previewService = previewService;
        this.generationService = generationService;
    }

    /**
     * 查询解析模板列表。
     *
     * @return 解析模板列表
     */
    @GetMapping
    public ApiResponse<List<ParserTemplateResponse>> listTemplates() {
        return ApiResponse.ok(managementService.listTemplates());
    }

    /**
     * 查询解析模板详情。
     *
     * @param id 模板主键
     * @return 解析模板详情
     */
    @GetMapping("/{id}")
    public ApiResponse<ParserTemplateResponse> getTemplate(@PathVariable Long id) {
        return ApiResponse.ok(managementService.getTemplate(id));
    }

    /**
     * 创建解析模板。
     *
     * @param request 保存请求
     * @return 创建后的模板
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ParserTemplateResponse>> createTemplate(@Valid @RequestBody ParserTemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("解析模板创建成功", managementService.createTemplate(request)));
    }

    /**
     * 更新解析模板。
     *
     * @param id 模板主键
     * @param request 保存请求
     * @return 更新后的模板
     */
    @PutMapping("/{id}")
    public ApiResponse<ParserTemplateResponse> updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody ParserTemplateRequest request) {
        return ApiResponse.ok("解析模板更新成功", managementService.updateTemplate(id, request));
    }

    /**
     * 删除解析模板。
     *
     * @param id 模板主键
     * @return 空响应
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteTemplate(@PathVariable Long id) {
        managementService.deleteTemplate(id);
        return ApiResponse.ok("解析模板删除成功", null);
    }

    /**
     * 启用或停用解析模板。
     *
     * @param id 模板主键
     * @param enabled 是否启用
     * @return 更新后的模板
     */
    @PatchMapping("/{id}/enabled")
    public ApiResponse<ParserTemplateResponse> setTemplateEnabled(
            @PathVariable Long id,
            @RequestParam boolean enabled) {
        return ApiResponse.ok(managementService.setTemplateEnabled(id, enabled));
    }

    /**
     * 预览解析模板效果。
     *
     * @param request 预览请求
     * @return 预览结果
     */
    @PostMapping("/preview")
    public ApiResponse<ParserTemplatePreviewResponse> preview(@Valid @RequestBody ParserTemplatePreviewRequest request) {
        return ApiResponse.ok(previewService.preview(request));
    }

    /**
     * 从真实订阅源样本生成解析模板。
     *
     * @param request 生成请求
     * @return 生成结果
     */
    @PostMapping("/generate-from-feed")
    public ApiResponse<ParserTemplateGenerateResponse> generateFromFeed(
            @Valid @RequestBody ParserTemplateGenerateRequest request) {
        return ApiResponse.ok("解析模板生成完成", generationService.generateFromFeed(request));
    }

    /**
     * 为订阅源绑定解析模板。
     *
     * @param feedId 订阅源主键
     * @param request 绑定请求
     * @return 绑定结果
     */
    @PutMapping("/feeds/{feedId}/binding")
    public ApiResponse<ParserTemplateFeedBindingResponse> bindFeedTemplate(
            @PathVariable Long feedId,
            @RequestBody BindParserTemplateRequest request) {
        return ApiResponse.ok(managementService.bindFeedTemplate(feedId, request));
    }
}
