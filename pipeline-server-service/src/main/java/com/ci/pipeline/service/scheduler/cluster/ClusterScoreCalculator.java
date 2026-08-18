package com.ci.pipeline.service.scheduler.cluster;

import com.ci.pipeline.common.constants.ClusterConstants;
import com.ci.pipeline.dao.entity.ClusterInfo;
import com.ci.pipeline.service.remote.ArgoWorkflowAgent;
import com.ci.pipeline.service.remote.KubernetesAgent;
import com.ci.pipeline.service.service.ClusterConfigService;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeCondition;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 单集群实时打分器。
 * <p>每次调度对候选集群并行调用，单集群打分链路（全部 fail-safe，任何异常 → 0 分出局）：
 * <ol>
 *   <li>K8s 节点查询：过滤 control-plane NoSchedule 节点 + NotReady 节点，无可调度节点 → 0 分；</li>
 *   <li>Argo 模板存在性检查：模板未同步到该集群 → 0 分（把问题挡在提交前）；</li>
 *   <li>节点内存采样：metrics.k8s.io，score = 可调度节点平均空闲内存占比；metrics 失败降级中性分 0.5；</li>
 *   <li>可选硬上限：maxRunningWorkflows 启用时运行数达到上限 → 0 分。</li>
 * </ol>
 */
@Slf4j
@Component
public class ClusterScoreCalculator {

    @Autowired
    private KubernetesAgent kubernetesAgent;

    @Autowired
    private ArgoWorkflowAgent argoWorkflowAgent;

    @Autowired
    private ClusterConfigService clusterConfigService;

    /**
     * 打分结果
     */
    @Data
    public static class ClusterScore {

        private String clusterName;
        /** 得分（0 表示不可用） */
        private double score;
        /** 分数是否来自 metrics 降级（中性分） */
        private boolean metricsStale;
        /** 不可用原因（score=0 时记录，用于日志/排查） */
        private String unavailableReason;

        public static ClusterScore unavailable(String clusterName, String reason) {
            ClusterScore result = new ClusterScore();
            result.setClusterName(clusterName);
            result.setScore(ClusterConstants.UNAVAILABLE_SCORE);
            result.setUnavailableReason(reason);
            return result;
        }

        public static ClusterScore of(String clusterName, double score, boolean metricsStale) {
            ClusterScore result = new ClusterScore();
            result.setClusterName(clusterName);
            result.setScore(score);
            result.setMetricsStale(metricsStale);
            return result;
        }
    }

    /**
     * 对单个集群打分（模板编码用于 Argo 模板存在性检查）。
     *
     * @param cluster      集群定义
     * @param templateCode 流水线模板编码
     * @return 打分结果（异常不抛出，统一 0 分）
     */
    public ClusterScore scoreCluster(ClusterInfo cluster, String templateCode) {
        String clusterName = cluster.getClusterName();
        try {
            // ① 节点查询 + 可调度过滤
            List<V1Node> nodes = kubernetesAgent.listNodes(clusterName);
            List<V1Node> schedulableNodes = filterSchedulableNodes(nodes);
            if (schedulableNodes.isEmpty()) {
                return ClusterScore.unavailable(clusterName, "无可调度节点");
            }

            // ② Argo 模板存在性检查（模板未同步到该集群 → 0 分）
            String namespace = clusterConfigService.getNamespace(clusterName);
            try {
                argoWorkflowAgent.getWorkflowTemplate(clusterName, namespace, templateCode);
            } catch (Exception e) {
                return ClusterScore.unavailable(clusterName, "模板未同步到该集群: " + templateCode);
            }

            // ③ 可选硬上限：运行中 Workflow 数
            if (cluster.getMaxRunningWorkflows() != null && cluster.getMaxRunningWorkflows() > 0) {
                try {
                    int running = argoWorkflowAgent
                            .listWorkflows(clusterName, namespace, java.util.Collections.singletonList("Running"))
                            .getItems() == null ? 0
                            : argoWorkflowAgent.listWorkflows(clusterName, namespace,
                                    java.util.Collections.singletonList("Running")).getItems().size();
                    if (running >= cluster.getMaxRunningWorkflows()) {
                        return ClusterScore.unavailable(clusterName,
                                String.format("运行中 Workflow 数达到上限: %d/%d", running, cluster.getMaxRunningWorkflows()));
                    }
                } catch (Exception e) {
                    log.warn("统计运行中 Workflow 数失败，跳过硬上限检查, clusterName={}", clusterName, e);
                }
            }

            // ④ 节点内存采样（metrics 失败降级中性分）
            return scoreByFreeMemory(clusterName, cluster, schedulableNodes);
        } catch (Exception e) {
            log.warn("集群打分失败（按不可用处理）, clusterName={}", clusterName, e);
            return ClusterScore.unavailable(clusterName, "打分异常: " + e.getMessage());
        }
    }

    /**
     * 节点空闲内存打分 + metrics 降级。
     */
    private ClusterScore scoreByFreeMemory(String clusterName, ClusterInfo cluster, List<V1Node> schedulableNodes) {
        Map<String, Long> usageMap;
        try {
            usageMap = kubernetesAgent.getNodeMemoryUsageBytes(clusterName);
        } catch (Exception e) {
            log.warn("metrics 查询失败，降级中性分, clusterName={}", clusterName, e);
            return ClusterScore.of(clusterName, ClusterConstants.NEUTRAL_SCORE_WHEN_METRICS_STALE, true);
        }

        // 缺 usage 的节点占比超过阈值 → 视为 metrics 异常，降级
        List<V1Node> nodesWithUsage = schedulableNodes.stream()
                .filter(n -> n.getMetadata() != null
                        && usageMap.containsKey(n.getMetadata().getName())
                        && usageMap.get(n.getMetadata().getName()) > 0)
                .collect(Collectors.toList());
        double missingRatio = 1D - (schedulableNodes.isEmpty() ? 0 : (double) nodesWithUsage.size() / schedulableNodes.size());
        if (nodesWithUsage.isEmpty() || missingRatio > ClusterConstants.METRICS_MISSING_RATIO_THRESHOLD) {
            log.warn("metrics 数据缺失过多（missingRatio={}），降级中性分, clusterName={}", missingRatio, clusterName);
            return ClusterScore.of(clusterName, ClusterConstants.NEUTRAL_SCORE_WHEN_METRICS_STALE, true);
        }

        // score = 平均空闲内存占比
        double score = nodesWithUsage.stream()
                .mapToDouble(node -> {
                    String nodeName = node.getMetadata().getName();
                    io.kubernetes.client.custom.Quantity allocatableQuantity =
                            node.getStatus() != null && node.getStatus().getAllocatable() != null
                                    ? node.getStatus().getAllocatable().get("memory") : null;
                    long allocatable = allocatableQuantity != null
                            ? allocatableQuantity.getNumber().longValue() : 0L;
                    long usage = usageMap.getOrDefault(nodeName, 0L);
                    if (allocatable <= 0) {
                        return 0D;
                    }
                    return (double) (allocatable - usage) / allocatable;
                })
                .average()
                .orElse(ClusterConstants.UNAVAILABLE_SCORE);
        return ClusterScore.of(clusterName, BigDecimal.valueOf(score).setScale(4, RoundingMode.HALF_UP).doubleValue(), false);
    }

    /**
     * 过滤可调度节点：剔除 control-plane NoSchedule 节点 + NotReady 节点。
     */
    private List<V1Node> filterSchedulableNodes(List<V1Node> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return nodes.stream()
                .filter(Objects::nonNull)
                .filter(this::isSchedulable)
                .collect(Collectors.toList());
    }

    private boolean isSchedulable(V1Node node) {
        V1ObjectMeta metadata = node.getMetadata();
        if (metadata == null || metadata.getLabels() == null) {
            return false;
        }
        // control-plane 且 NoSchedule 的节点剔除
        String controlPlane = metadata.getLabels().get(ClusterConstants.NODE_ROLE_CONTROL_PLANE);
        if (ClusterConstants.TAINT_NO_SCHEDULE.equalsIgnoreCase(controlPlane)) {
            return false;
        }
        // Ready condition 必须 True（NotReady 节点对象还在但不可调度，会虚增得分）
        if (node.getStatus() == null || node.getStatus().getConditions() == null) {
            return false;
        }
        boolean ready = node.getStatus().getConditions().stream()
                .filter(Objects::nonNull)
                .filter(c -> ClusterConstants.NODE_CONDITION_READY.equals(c.getType()))
                .map(V1NodeCondition::getStatus)
                .anyMatch(ClusterConstants.NODE_CONDITION_TRUE::equals);
        return ready;
    }
}
