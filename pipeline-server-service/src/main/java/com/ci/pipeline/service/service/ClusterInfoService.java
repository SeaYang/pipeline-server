package com.ci.pipeline.service.service;

import com.ci.pipeline.facade.request.ClusterInfoCreateRequest;
import com.ci.pipeline.facade.request.ClusterInfoQueryRequest;
import com.ci.pipeline.facade.request.ClusterInfoUpdateRequest;
import com.ci.pipeline.facade.request.ClusterTestConnectionRequest;
import com.ci.pipeline.facade.response.ClusterInfoResponse;
import com.ci.pipeline.facade.response.ClusterTestConnectionResponse;
import com.ci.pipeline.facade.response.PageResponse;

/**
 * 集群管理服务（CRUD + 摘流开关 + 测试连接）
 */
public interface ClusterInfoService {

    /**
     * 分页查询集群列表
     */
    PageResponse<ClusterInfoResponse> page(ClusterInfoQueryRequest query);

    /**
     * 新增集群（autoSyncTemplates=true 时异步全量同步模板）
     */
    ClusterInfoResponse create(ClusterInfoCreateRequest request);

    /**
     * 编辑集群（clusterName 不可改；token 留空表示不修改）
     */
    ClusterInfoResponse update(ClusterInfoUpdateRequest request);

    /**
     * 逻辑删除集群（被 pipeline_run 引用时拦截）
     */
    void delete(Long id);

    /**
     * 摘流开关切换（online 0/1）
     */
    void toggleOnline(String clusterName, boolean online);

    /**
     * 测试连接：用表单参数实时构建临时客户端探测 Argo / K8s（不落库）
     */
    ClusterTestConnectionResponse testConnection(ClusterTestConnectionRequest request);
}
