package com.ci.pipeline.service.scheduler.cluster;

import com.ci.pipeline.common.constants.ClusterConstants;
import com.ci.pipeline.common.exception.BusinessException;
import com.ci.pipeline.dao.entity.ClusterInfo;
import com.ci.pipeline.dao.entity.PipelineTemplate;
import com.ci.pipeline.service.service.ClusterConfigService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 调度策略抽象基类：候选范围构造 + 并行实时打分 + 最高分选择。
 * <p>候选范围 = enabled ∩ online 的集群；打分并行执行（整体耗时 = 最慢集群，
 * 正常 < 1s，死集群等满 connect 超时 5s），不随集群数线性增长。
 */
@Slf4j
public abstract class AbstractClusterScheduler implements ClusterScheduler {

    @Autowired
    protected ClusterConfigService clusterConfigService;

    @Autowired
    protected ClusterScoreCalculator clusterScoreCalculator;

    /**
     * 解析模板的候选集群名列表（cluster_names 逗号分隔）
     */
    protected List<String> resolveTemplateClusterNames(PipelineTemplate template) {
        return clusterConfigService.splitClusterNames(template.getClusterNames());
    }

    /**
     * 对候选范围并行实时打分，返回可用集群（score > 0 且达水位）按得分降序排列。
     */
    protected List<ClusterScoreCalculator.ClusterScore> scoreAvailableClusters(String templateCode) {
        List<ClusterInfo> candidates = clusterConfigService.listSchedulable();
        if (candidates.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        Map<String, ClusterInfo> candidateMap = candidates.stream()
                .collect(Collectors.toMap(ClusterInfo::getClusterName, c -> c));

        // 并行打分（候选集群数少，parallelStream 足够）
        List<ClusterScoreCalculator.ClusterScore> scores = candidates.parallelStream()
                .map(cluster -> clusterScoreCalculator.scoreCluster(cluster, templateCode))
                .collect(Collectors.toList());

        // 可用 = score > 0 且（达水位 或 metrics 降级中性分）
        List<ClusterScoreCalculator.ClusterScore> available = scores.stream()
                .filter(s -> s.getScore() > ClusterConstants.UNAVAILABLE_SCORE)
                .filter(s -> s.isMetricsStale() || meetThreshold(candidateMap.get(s.getClusterName()), s.getScore()))
                .sorted(Comparator.comparingDouble(ClusterScoreCalculator.ClusterScore::getScore).reversed())
                .collect(Collectors.toList());

        log.info("集群打分完成, templateCode={}, candidates={}, scores={}, available={}",
                templateCode,
                candidates.stream().map(ClusterInfo::getClusterName).collect(Collectors.toList()),
                scores.stream().map(s -> s.getClusterName() + "=" + s.getScore()
                        + (s.isMetricsStale() ? "(stale)" : "")
                        + (StringUtils.isNotBlank(s.getUnavailableReason()) ? "(" + s.getUnavailableReason() + ")" : ""))
                        .collect(Collectors.toList()),
                available.stream().map(ClusterScoreCalculator.ClusterScore::getClusterName).collect(Collectors.toList()));
        return available;
    }

    /**
     * 水位判断：score ≥ 集群配置的 freeMemoryThreshold（默认 0.2）
     */
    private boolean meetThreshold(ClusterInfo cluster, double score) {
        double threshold = cluster.getFreeMemoryThreshold() != null
                ? cluster.getFreeMemoryThreshold().doubleValue()
                : ClusterConstants.DEFAULT_FREE_MEMORY_THRESHOLD;
        return score >= threshold;
    }

    /**
     * 从得分列表中选最高分（并列取默认集群优先，再取字典序保证稳定）
     */
    protected String pickHighest(List<ClusterScoreCalculator.ClusterScore> scores) {
        if (scores == null || scores.isEmpty()) {
            return null;
        }
        double highest = scores.get(0).getScore();
        List<ClusterScoreCalculator.ClusterScore> tied = scores.stream()
                .filter(s -> s.getScore() == highest)
                .collect(Collectors.toList());
        String defaultClusterName = safeDefaultClusterName();
        Optional<ClusterScoreCalculator.ClusterScore> defaultFirst = tied.stream()
                .filter(s -> s.getClusterName().equals(defaultClusterName))
                .findFirst();
        return defaultFirst.map(ClusterScoreCalculator.ClusterScore::getClusterName)
                .orElseGet(() -> tied.stream()
                        .map(ClusterScoreCalculator.ClusterScore::getClusterName)
                        .sorted()
                        .findFirst()
                        .orElse(null));
    }

    /**
     * 默认集群名（无默认集群配置时不影响选择，返回 null）
     */
    protected String safeDefaultClusterName() {
        try {
            return clusterConfigService.getDefaultClusterName();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 无可用集群统一异常
     */
    protected BusinessException noAvailableCluster() {
        return new BusinessException("当前暂无可用执行集群，请检查集群配置与在线状态");
    }

    /**
     * 模板编码兜底（template 为空场景）
     */
    protected String templateCodeOf(PipelineTemplate template) {
        return template != null ? template.getPipelineTemplateCode() : "";
    }
}
