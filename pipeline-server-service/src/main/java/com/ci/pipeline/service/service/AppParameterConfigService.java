package com.ci.pipeline.service.service;

import com.ci.pipeline.facade.request.AppParameterConfigBatchCreateRequest;
import com.ci.pipeline.facade.request.AppParameterConfigCreateRequest;
import com.ci.pipeline.facade.request.AppParameterConfigQueryRequest;
import com.ci.pipeline.facade.request.AppParameterConfigUpdateRequest;
import com.ci.pipeline.facade.response.AppParameterConfigResponse;

import java.util.List;

public interface AppParameterConfigService {

    AppParameterConfigResponse create(AppParameterConfigCreateRequest request);

    void batchCreate(AppParameterConfigBatchCreateRequest request);

    AppParameterConfigResponse update(AppParameterConfigUpdateRequest request);

    void deleteById(Long id);

    List<AppParameterConfigResponse> list(AppParameterConfigQueryRequest query);

    List<String> listEnvs();

    /**
     * 策略查询：先查指定 env，兜底查 default env。
     *
     * @param appName       应用名称
     * @param parameterName 参数名
     * @param env           环境（可为 null）
     * @return 配置值，未查到返回 null
     */
    String getValue(String appName, String parameterName, String env);
}
