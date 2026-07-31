package com.ci.pipeline.service.service;

import com.ci.pipeline.facade.request.PipelineParameterCreateRequest;
import com.ci.pipeline.facade.request.PipelineParameterQueryRequest;
import com.ci.pipeline.facade.request.PipelineParameterUpdateRequest;
import com.ci.pipeline.facade.request.PipelineParametersRefreshRequest;
import com.ci.pipeline.facade.request.PipelineParametersRequest;
import com.ci.pipeline.facade.response.AppParameterOptionResponse;
import com.ci.pipeline.facade.response.PageResponse;
import com.ci.pipeline.facade.response.PipelineParameterResponse;
import com.ci.pipeline.facade.response.PipelineRunParameterResponse;

import java.util.List;

public interface PipelineParameterService {

    PipelineParameterResponse create(PipelineParameterCreateRequest request);

    PipelineParameterResponse update(PipelineParameterUpdateRequest request);

    void deleteById(Long id);

    PipelineParameterResponse getById(Long id);

    /**
     * 按参数名查询详情。
     *
     * @param name 参数名
     * @return 参数定义响应
     */
    PipelineParameterResponse getByName(String name);

    PageResponse<PipelineParameterResponse> page(PipelineParameterQueryRequest query);

    /**
     * 查询全部参数定义（仅 name 和 label），用于依赖参数选择等场景。
     *
     * @return 参数定义响应列表（仅含 name、label）
     */
    List<PipelineParameterResponse> listAllSimple();

    /**
     * 解析流水线执行参数列表（执行弹框参数加载）。
     * <p>从生效模板版本解析参数名，关联参数定义表（递归加载依赖参数），拓扑排序后逐个计算初始值，
     * 过滤 system 参数，只返回 user 参数。
     *
     * @param request 参数请求（含 pipelineId，可选 currentValues 用于第三方传值预填）
     * @return 用户参数列表（含初始值和选项）
     */
    List<PipelineRunParameterResponse> listRunParameters(PipelineParametersRequest request);

    /**
     * 刷新流水线执行参数（参数联动刷新）。
     * <p>当某个标记了 refreshOnChanged 的参数值变动时，清除其下游参数的旧值（重置为默认值或清空），
     * 然后重新计算全部参数，返回全量 user 参数列表（前端整体替换）。
     *
     * @param request 刷新请求（含 pipelineId、变动参数名和当前所有参数值）
     * @return 全量 user 参数列表（含重新计算的值和过滤后的选项）
     */
    List<PipelineRunParameterResponse> refreshParameters(PipelineParametersRefreshRequest request);

    /**
     * 查询可配置的参数列表（component_type 为 INPUT/SELECT/RADIO/GIT_TREE 且 param_type 为 user）。
     *
     * @return 可配置参数选项列表
     */
    List<AppParameterOptionResponse> listConfigurableParameters();
}
