package com.ci.pipeline.service.service;

import java.util.Map;

/**
 * 流水线事件触发核心服务。
 * <p>负责：模板匹配 → pipeline 自动创建/复用 → 执行流水线
 */
public interface PipelineEventService {

    /**
     * 事件触发并执行流水线。
     * <p>核心流程：
     * <ol>
     *   <li>根据 eventType 查 pipeline_template_event_bind 获取模板列表</li>
     *   <li>根据 appName 查 app_info 获取编程语言，过滤匹配的模板</li>
     *   <li>查模板的生效版本（EFFECTIVE）</li>
     *   <li>查 pipeline_event_bind，存在则复用 pipelineId，不存在则创建 pipeline + 绑定</li>
     *   <li>调用参数接口获取完整参数（合并 API 传入值与默认值）</li>
     *   <li>调用 PipelineService.execute 执行流水线</li>
     * </ol>
     *
     * @param eventType 事件类型
     * @param appName   应用名称
     * @param params    触发参数（含 git-branch 等）
     * @return pipelineRunId 流水线运行记录 ID
     */
    Long triggerAndExecute(String eventType, String appName, Map<String, String> params);
}
