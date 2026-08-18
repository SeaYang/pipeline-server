package com.ci.pipeline.facade.response;

import lombok.Data;

import java.io.Serializable;

/**
 * 集群测试连接响应（各探测项独立返回结果与耗时）
 */
@Data
public class ClusterTestConnectionResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Argo Server 探测是否成功
     */
    private Boolean argoOk;

    /**
     * Argo Server 探测信息（成功为版本信息，失败为异常摘要）
     */
    private String argoMessage;

    /**
     * Argo 探测耗时毫秒
     */
    private Long argoCostMs;

    /**
     * K8s API Server 探测是否成功
     */
    private Boolean k8sOk;

    /**
     * K8s 探测信息（成功为节点数，失败为异常摘要）
     */
    private String k8sMessage;

    /**
     * K8s 探测耗时毫秒
     */
    private Long k8sCostMs;

    /**
     * 整体是否可保存（两项探测都成功才建议保存）
     */
    private Boolean allOk;
}
