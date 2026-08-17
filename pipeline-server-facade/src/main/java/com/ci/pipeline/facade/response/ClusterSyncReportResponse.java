package com.ci.pipeline.facade.response;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 集群全量模板同步报告（新集群接入 / 手动触发全量同步）
 */
@Data
public class ClusterSyncReportResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 集群名
     */
    private String clusterName;

    /**
     * 同步的模板总数（流水线模板 + 任务模板）
     */
    private Integer total;

    /**
     * 成功数
     */
    private Integer successCount;

    /**
     * 失败数
     */
    private Integer failureCount;

    /**
     * 失败明细
     */
    private List<ClusterSyncResultResponse> failures = new ArrayList<>();

    /**
     * 是否全部成功
     */
    public boolean isAllSuccess() {
        return failureCount != null && failureCount == 0;
    }
}
