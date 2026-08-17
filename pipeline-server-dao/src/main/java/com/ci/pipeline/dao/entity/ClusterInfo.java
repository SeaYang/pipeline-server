package com.ci.pipeline.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 执行集群定义实体（每个集群对应一套 K8s + Argo Workflows）。
 * <p>集群配置运行时热生效：读取侧带内容指纹缓存，配置变更后客户端自动重建。
 */
@Data
@TableName("cluster_info")
public class ClusterInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 集群唯一标识（小写字母数字中划线），被 pipeline_template.cluster_names / pipeline_run.cluster_name 引用
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
     * Argo 认证 token（含 Bearer 前缀）
     */
    private String argoToken;

    /**
     * Workflow / WorkflowTemplate 所在命名空间
     */
    private String argoNamespace;

    /**
     * K8s API Server 地址
     */
    private String k8sMasterUrl;

    /**
     * K8s 认证 token（不含 Bearer 前缀）
     */
    private String k8sToken;

    /**
     * 是否校验 K8s 证书（0-不校验，1-校验）
     */
    private Integer k8sVerifyingSsl;

    /**
     * 连接超时（毫秒）
     */
    private Integer connectTimeoutMs;

    /**
     * 读取超时（毫秒）
     */
    private Integer readTimeoutMs;

    /**
     * 调度准入水位：平均空闲内存占比低于该值不参与调度
     */
    private BigDecimal freeMemoryThreshold;

    /**
     * 运行中 Workflow 数硬上限（NULL 不启用）
     */
    private Integer maxRunningWorkflows;

    /**
     * 集群生命周期（1-启用，0-下线：不调度、不同步模板）
     */
    private Integer enabled;

    /**
     * 调度摘流开关（1-在线，0-摘流：不调度但模板继续同步）
     */
    private Integer online;

    /**
     * 默认集群标记（1-默认，全局唯一；存量 run cluster_name 为空时路由兜底）
     */
    private Integer isDefault;

    /**
     * 乐观锁版本号
     */
    private Integer revision;

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

    /**
     * 逻辑删除标识（0-未删除，1-已删除），由 MyBatis-Plus 全局配置接管
     */
    private Integer deleted;
}
