package com.ci.pipeline.service.controller;

import com.ci.pipeline.common.auth.RequireLogin;
import com.ci.pipeline.common.result.Result;
import com.ci.pipeline.facade.request.DictTypeCreateRequest;
import com.ci.pipeline.facade.request.DictTypeQueryRequest;
import com.ci.pipeline.facade.request.DictTypeUpdateRequest;
import com.ci.pipeline.facade.response.DictTypeResponse;
import com.ci.pipeline.facade.response.PageResponse;
import com.ci.pipeline.service.service.DictTypeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 字典类型控制器
 */
@Slf4j
@RestController
@RequestMapping("/dict/type")
@RequireLogin
public class DictTypeController {

    @Autowired
    private DictTypeService dictTypeService;

    /**
     * 新增字典类型
     */
    @PostMapping
    public Result<DictTypeResponse> create(@RequestBody DictTypeCreateRequest request) {
        return Result.success(dictTypeService.create(request));
    }

    /**
     * 修改字典类型
     */
    @PutMapping
    public Result<DictTypeResponse> update(@RequestBody DictTypeUpdateRequest request) {
        return Result.success(dictTypeService.update(request));
    }

    /**
     * 删除字典类型（若存在字典数据则禁止删除）
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        dictTypeService.deleteById(id);
        return Result.success();
    }

    /**
     * 根据主键查询字典类型
     */
    @GetMapping("/{id}")
    public Result<DictTypeResponse> get(@PathVariable("id") Long id) {
        return Result.success(dictTypeService.getById(id));
    }

    /**
     * 分页查询字典类型
     */
    @GetMapping("/page")
    public Result<PageResponse<DictTypeResponse>> page(DictTypeQueryRequest query) {
        return Result.success(dictTypeService.page(query));
    }
}
