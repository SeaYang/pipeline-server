package com.ci.pipeline.facade.response;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 集群信息响应（token 脱敏，只回显后 4 位）
 */
@Data
public class ClusterInfoResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 集群唯一标识
     */
    private String clusterName;

    /**
     * 集群描述
     */
    private String description;

    /**
     * Argo Server 地址
     */
    private String argoUrl;

    /**
     * Argo 认证 token 脱敏回显（如 ****abcd）
     */
    private String argoTokenMasked;

    /**
     * Workflow / WorkflowTemplate 所在命名空间
     */
    private String argoNamespace;

    /**
     * K8s API Server 地址
     */
    private String k8sMasterUrl;

    /**
     * K8s 认证 token 脱敏回显
     */
    private String k8sTokenMasked;

    /**
     * 是否校验 K8s 证书
     */
    private Boolean k8sVerifyingSsl;

    /**
     * 连接超时毫秒
     */
    private Integer connectTimeoutMs;

    /**
     * 读取超时毫秒
     */
    private Integer readTimeoutMs;

    /**
     * 调度准入水位
     */
    private BigDecimal freeMemoryThreshold;

    /**
     * 运行中 Workflow 数硬上限（null 不启用）
     */
    private Integer maxRunningWorkflows;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 是否在线
     */
    private Boolean online;

    /**
     * 是否默认集群
     */
    private Boolean isDefault;

    /**
     * 创建人
     */
    private String creator;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 最后修改人
     */
    private String updater;

    /**
     * 更新时间
     */
    private Date updateTime;
}
