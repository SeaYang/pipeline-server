package com.ci.pipeline.service.service;

import com.ci.pipeline.facade.request.DictTypeCreateRequest;
import com.ci.pipeline.facade.request.DictTypeQueryRequest;
import com.ci.pipeline.facade.request.DictTypeUpdateRequest;
import com.ci.pipeline.facade.response.DictTypeResponse;
import com.ci.pipeline.facade.response.PageResponse;

/**
 * 字典类型业务接口
 */
public interface DictTypeService {

    /**
     * 新增字典类型
     */
    DictTypeResponse create(DictTypeCreateRequest request);

    /**
     * 修改字典类型
     */
    DictTypeResponse update(DictTypeUpdateRequest request);

    /**
     * 根据主键删除字典类型（若存在字典数据则禁止删除）
     */
    void deleteById(Long id);

    /**
     * 根据主键查询字典类型
     */
    DictTypeResponse getById(Long id);

    /**
     * 分页查询字典类型
     */
    PageResponse<DictTypeResponse> page(DictTypeQueryRequest query);
}
