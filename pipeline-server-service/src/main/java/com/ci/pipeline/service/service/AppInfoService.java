package com.ci.pipeline.service.service;

import com.ci.pipeline.facade.request.AppInfoCreateRequest;
import com.ci.pipeline.facade.request.AppInfoQueryRequest;
import com.ci.pipeline.facade.request.AppInfoUpdateRequest;
import com.ci.pipeline.facade.response.AppInfoResponse;
import com.ci.pipeline.facade.response.PageResponse;

/**
 * 应用基础信息业务接口
 */
public interface AppInfoService {

    /**
     * 根据应用名称查询应用详情。
     *
     * @param appName 应用名称
     * @return 应用详情
     */
    AppInfoResponse getByAppName(String appName);

    /**
     * 新增应用
     */
    AppInfoResponse create(AppInfoCreateRequest request);

    /**
     * 修改应用
     */
    AppInfoResponse update(AppInfoUpdateRequest request);

    /**
     * 根据主键删除应用
     */
    void deleteById(Long id);

    /**
     * 根据主键查询应用
     */
    AppInfoResponse getById(Long id);

    /**
     * 分页查询应用
     */
    PageResponse<AppInfoResponse> page(AppInfoQueryRequest query);
}
