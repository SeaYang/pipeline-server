package com.ci.pipeline.service.scheduler.cluster;

import com.ci.pipeline.dao.entity.PipelineTemplate;

/**
 * 集群调度策略接口（为流水线模板选择执行集群）
 */
public interface ClusterScheduler {

    /**
     * 为流水线模板选择执行集群。
     *
     * @param template 流水线模板（含 clusterNames / clusterSchedulePolicy）
     * @return 选中的集群名
     * @throws com.ci.pipeline.common.exception.BusinessException 无可用集群时抛出
     */
    String selectCluster(PipelineTemplate template);
}
