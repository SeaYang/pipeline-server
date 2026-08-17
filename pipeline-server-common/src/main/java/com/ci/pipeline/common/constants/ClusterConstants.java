package com.ci.pipeline.common.constants;

/**
 * 多集群调度相关常量定义
 */
public final class ClusterConstants {

    private ClusterConstants() {
    }

    /**
     * pipeline_template.cluster_names 字段的分隔符（多个集群名逗号分隔存储）
     */
    public static final String CLUSTER_NAMES_SEPARATOR = ",";

    /**
     * 默认集群调度策略（pipeline_template.cluster_schedule_policy 为空时的兜底值）
     */
    public static final String DEFAULT_SCHEDULE_POLICY = "Any";

    /**
     * 默认空闲内存准入水位（集群平均空闲内存占比低于该值不参与调度）
     */
    public static final double DEFAULT_FREE_MEMORY_THRESHOLD = 0.2D;

    /**
     * 默认连接超时（毫秒）
     */
    public static final int DEFAULT_CONNECT_TIMEOUT_MS = 5000;

    /**
     * 默认读取超时（毫秒）
     */
    public static final int DEFAULT_READ_TIMEOUT_MS = 10000;

    /**
     * 集群名格式（小写字母数字中划线，长度 1~100）
     */
    public static final String CLUSTER_NAME_PATTERN = "^[a-z0-9-]{1,100}$";

    /**
     * yml 兜底集群名（cluster_info 表为空时由 yml 配置合成的集群标识，不落库）
     */
    public static final String FALLBACK_CLUSTER_NAME = "default";

    /**
     * K8s control-plane 角色标签 key
     */
    public static final String NODE_ROLE_CONTROL_PLANE = "node-role.kubernetes.io/control-plane";

    /**
     * K8s NoSchedule 污点/标签效果值
     */
    public static final String TAINT_NO_SCHEDULE = "NoSchedule";

    /**
     * 节点 Ready condition 类型
     */
    public static final String NODE_CONDITION_READY = "Ready";

    /**
     * 节点 Ready condition 正常状态值
     */
    public static final String NODE_CONDITION_TRUE = "True";

    /**
     * 节点 metrics API 路径（metrics.k8s.io，由 metrics-server 提供）
     */
    public static final String NODE_METRICS_API_PATH = "apis/metrics.k8s.io/v1beta1";

    /**
     * 节点 metrics 资源复数名
     */
    public static final String NODE_METRICS_RESOURCE_PLURAL = "nodes";

    /**
     * metrics 降级时的中性分（既不优先也不淘汰，集群可用性由节点查询/模板存在性决定）
     */
    public static final double NEUTRAL_SCORE_WHEN_METRICS_STALE = 0.5D;

    /**
     * metrics 降级判定阈值：缺失 usage 的节点占比超过该值视为 metrics 异常
     */
    public static final double METRICS_MISSING_RATIO_THRESHOLD = 0.5D;

    /**
     * 集群不可用（打分 0 分）的统一标记分数
     */
    public static final double UNAVAILABLE_SCORE = 0D;

    /**
     * 集群同步操作类型：保存（create-or-update）
     */
    public static final String SYNC_ACTION_SAVE = "SAVE";

    /**
     * 集群同步操作类型：删除
     */
    public static final String SYNC_ACTION_DELETE = "DELETE";
}
