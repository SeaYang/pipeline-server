package com.ci.pipeline.facade.response;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 模板多集群同步结果响应（单模板单集群粒度）
 */
@Data
public class ClusterSyncResultResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 集群名
     */
    private String clusterName;

    /**
     * 同步是否成功
     */
    private Boolean success;

    /**
     * 失败原因（成功时为空）
     */
    private String errorMessage;

    public static ClusterSyncResultResponse success(String clusterName) {
        ClusterSyncResultResponse response = new ClusterSyncResultResponse();
        response.setClusterName(clusterName);
        response.setSuccess(true);
        return response;
    }

    public static ClusterSyncResultResponse failure(String clusterName, String errorMessage) {
        ClusterSyncResultResponse response = new ClusterSyncResultResponse();
        response.setClusterName(clusterName);
        response.setSuccess(false);
        response.setErrorMessage(errorMessage);
        return response;
    }
}
