package com.ci.pipeline.service.strategy;

import com.ci.pipeline.dao.entity.PipelineRun;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 参数计算上下文，在参数解析引擎中逐步传递和填充。
 * <p>包含流水线上下文信息（pipelineId、appName 等）以及计算过程中逐步填充的已解析参数值。
 */
@Data
@Builder
public class ParamResolveContext {

    /** 流水线 id */
    private Long pipelineId;

    /** 应用名称 */
    private String appName;

    /** 流水线模板编码 */
    private String pipelineTemplateCode;

    /** Argo 命名空间（预留，当前未使用） */
    private String namespace;

    /** 已计算的参数值，key=参数名，value=参数值，计算过程中逐步填充 */
    private Map<String, String> resolvedValues;

    /**
     * 最近一次成功执行记录的缓存（避免多个参数重复查询）。
     * <p>使用惰性加载：首次访问时填充，后续策略直接复用。
     * 值为 null 表示已查询过但不存在成功记录；未初始化时用 {@link #latestRunLoaded} 区分。
     */
    private PipelineRun cachedLatestSuccessfulRun;

    /** 标记 cachedLatestSuccessfulRun 是否已加载（区分"未查询"和"查询结果为 null"） */
    private boolean latestRunLoaded;
}
