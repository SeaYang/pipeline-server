package com.ci.pipeline.service.controller;

import com.ci.pipeline.common.result.Result;
import com.ci.pipeline.facade.request.DictDataCreateRequest;
import com.ci.pipeline.facade.request.DictDataQueryRequest;
import com.ci.pipeline.facade.request.DictDataUpdateRequest;
import com.ci.pipeline.facade.response.DictDataResponse;
import com.ci.pipeline.facade.response.PageResponse;
import com.ci.pipeline.service.service.DictDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 字典数据控制器
 */
@Slf4j
@RestController
@RequestMapping("/dict/data")
public class DictDataController {

    @Autowired
    private DictDataService dictDataService;

    /**
     * 新增字典数据
     */
    @PostMapping
    public Result<DictDataResponse> create(@RequestBody DictDataCreateRequest request) {
        return Result.success(dictDataService.create(request));
    }

    /**
     * 修改字典数据
     */
    @PutMapping
    public Result<DictDataResponse> update(@RequestBody DictDataUpdateRequest request) {
        return Result.success(dictDataService.update(request));
    }

    /**
     * 删除字典数据
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        dictDataService.deleteById(id);
        return Result.success();
    }

    /**
     * 根据主键查询字典数据
     */
    @GetMapping("/{id}")
    public Result<DictDataResponse> get(@PathVariable("id") Long id) {
        return Result.success(dictDataService.getById(id));
    }

    /**
     * 查询指定字典类型下的全部数据（按排序值升序，用于下拉框 / 枚举渲染）
     */
    @GetMapping("/list")
    public Result<List<DictDataResponse>> list(@RequestParam("dictType") String dictType) {
        return Result.success(dictDataService.listByDictType(dictType));
    }

    /**
     * 分页查询字典数据（支持 dictType 精确、dictKey / dictValue 模糊）
     */
    @GetMapping("/page")
    public Result<PageResponse<DictDataResponse>> page(DictDataQueryRequest query) {
        return Result.success(dictDataService.page(query));
    }
}
