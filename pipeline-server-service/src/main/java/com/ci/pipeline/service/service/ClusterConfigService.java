package com.ci.pipeline.service.service;

import com.ci.pipeline.dao.entity.ClusterInfo;

import java.util.List;

/**
 * 集群配置读取服务（带内容指纹缓存，配置变更后客户端自动重建）
 */
public interface ClusterConfigService {

    /**
     * 全部未删除集群（含禁用，管理页用）
     */
    List<ClusterInfo> listAll();

    /**
     * 启用（enabled=1）集群：模板同步范围
     */
    List<ClusterInfo> listEnabled();

    /**
     * 可调度集群（enabled=1 且 online=1）：调度候选范围
     */
    List<ClusterInfo> listSchedulable();

    /**
     * 按集群名查找（不存在抛 BusinessException）
     */
    ClusterInfo getByClusterName(String clusterName);

    /**
     * 默认集群名（is_default 集群；无则取第一条 enabled；再无抛 BusinessException）。
     * 用途：存量 pipeline_run.cluster_name 为空时的路由兜底。
     */
    String getDefaultClusterName();

    /**
     * 指定集群的 Argo namespace
     */
    String getNamespace(String clusterName);

    /**
     * 集群名列表与逗号分隔字符串互转（pipeline_template.cluster_names 存储格式）
     */
    String joinClusterNames(List<String> clusterNames);

    List<String> splitClusterNames(String clusterNames);

    /**
     * run 路由解析：run.clusterName 为空（存量数据）时兜底默认集群。
     * 各 run 相关服务（同步/日志/重试/停止/详情）统一使用，避免各自实现不一致。
     */
    String resolveRunClusterName(com.ci.pipeline.dao.entity.PipelineRun run);

    /**
     * run 的 Argo namespace（按解析出的集群实时读取集群配置）
     */
    String resolveRunNamespace(com.ci.pipeline.dao.entity.PipelineRun run);
}
