package com.ci.pipeline.service.service;

import com.ci.pipeline.facade.request.GenericConfigCreateRequest;
import com.ci.pipeline.facade.request.GenericConfigHistoryQueryRequest;
import com.ci.pipeline.facade.request.GenericConfigUpdateRequest;
import com.ci.pipeline.facade.response.GenericConfigHistoryResponse;
import com.ci.pipeline.facade.response.GenericConfigResponse;
import com.ci.pipeline.facade.response.PageResponse;

import java.util.List;

public interface GenericConfigService {

    /**
     * 查询全部配置（支持按 key 模糊搜索）。
     *
     * @param configKey 配置键（可选，模糊匹配）
     * @return 配置列表
     */
    List<GenericConfigResponse> list(String configKey);

    /**
     * 按主键查询配置详情。
     *
     * @param id 主键
     * @return 配置响应
     */
    GenericConfigResponse getById(Long id);

    /**
     * 按 key 取值（运行时调用）。
     *
     * @param configKey 配置键
     * @return 配置值（json 格式时返回解析后的对象）
     */
    Object getValueByKey(String configKey);

    /**
     * 新建配置。
     *
     * @param request 创建请求
     * @return 配置响应
     */
    GenericConfigResponse create(GenericConfigCreateRequest request);

    /**
     * 修改配置。
     *
     * @param request 更新请求
     * @return 配置响应
     */
    GenericConfigResponse update(GenericConfigUpdateRequest request);

    /**
     * 删除配置（逻辑删除）。
     *
     * @param id 主键
     */
    void delete(Long id);

    /**
     * 查询单条配置的变更历史。
     *
     * @param configId 配置ID
     * @return 历史列表
     */
    List<GenericConfigHistoryResponse> historyByConfigId(Long configId);

    /**
     * 分页查询全局变更历史。
     *
     * @param query 查询请求
     * @return 分页结果
     */
    PageResponse<GenericConfigHistoryResponse> historyPage(GenericConfigHistoryQueryRequest query);
}
