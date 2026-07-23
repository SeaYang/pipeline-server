package com.ci.pipeline.service.service;

import com.ci.pipeline.facade.request.DictDataCreateRequest;
import com.ci.pipeline.facade.request.DictDataQueryRequest;
import com.ci.pipeline.facade.request.DictDataUpdateRequest;
import com.ci.pipeline.facade.response.DictDataResponse;
import com.ci.pipeline.facade.response.PageResponse;

import java.util.List;

/**
 * 字典数据业务接口
 */
public interface DictDataService {

    /**
     * 新增字典数据
     */
    DictDataResponse create(DictDataCreateRequest request);

    /**
     * 修改字典数据
     */
    DictDataResponse update(DictDataUpdateRequest request);

    /**
     * 根据主键删除字典数据
     */
    void deleteById(Long id);

    /**
     * 根据主键查询字典数据
     */
    DictDataResponse getById(Long id);

    /**
     * 查询指定字典类型下的全部数据（按排序值升序）
     */
    List<DictDataResponse> listByDictType(String dictType);

    /**
     * 分页查询字典数据（支持 dictType 精确、dictKey / dictValue 模糊）
     */
    PageResponse<DictDataResponse> page(DictDataQueryRequest query);
}
