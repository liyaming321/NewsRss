package com.newsrss.controller;

import com.newsrss.dto.common.ApiResponse;
import com.newsrss.dto.dictionary.DictionaryRequest;
import com.newsrss.dto.dictionary.DictionaryResponse;
import com.newsrss.service.dictionary.DictionaryService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统字典接口。
 */
@RestController
@Profile("db")
@RequestMapping("/api/dictionaries")
public class DictionaryController {

    private final DictionaryService dictionaryService;

    public DictionaryController(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    /**
     * 查询字典项列表。
     *
     * @param dictType 字典类型
     * @param enabled 是否只查询启用项
     * @return 字典项列表
     */
    @GetMapping
    public ApiResponse<List<DictionaryResponse>> listItems(
            @RequestParam String dictType,
            @RequestParam(required = false) Boolean enabled) {
        return ApiResponse.ok(dictionaryService.listItems(dictType, enabled));
    }

    /**
     * 创建字典项。
     *
     * @param dictType 字典类型
     * @param request 保存请求
     * @return 创建后的字典项
     */
    @PostMapping
    public ResponseEntity<ApiResponse<DictionaryResponse>> createItem(
            @RequestParam String dictType,
            @Valid @RequestBody DictionaryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("字典项创建成功", dictionaryService.createItem(dictType, request)));
    }

    /**
     * 更新字典项。
     *
     * @param id 字典项主键
     * @param request 保存请求
     * @return 更新后的字典项
     */
    @PutMapping("/{id}")
    public ApiResponse<DictionaryResponse> updateItem(
            @PathVariable Long id,
            @Valid @RequestBody DictionaryRequest request) {
        return ApiResponse.ok("字典项更新成功", dictionaryService.updateItem(id, request));
    }

    /**
     * 删除字典项。
     *
     * @param id 字典项主键
     * @return 空响应
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteItem(@PathVariable Long id) {
        dictionaryService.deleteItem(id);
        return ApiResponse.ok("字典项删除成功", null);
    }
}
