package com.ci.pipeline.service.scheduler.cluster;

import com.ci.pipeline.dao.entity.PipelineTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * PreferSelected 策略：优先选中集群。
 * <p>优先在模板配置的候选集群（cluster_names ∩ 可用集群）中选得分最高者；
 * 优先池为空（选中集群全部不可用/未配置/被摘流）时兜底其他可用集群（可用性优先，自动溢出）。
 */
@Slf4j
@Component("PreferSelectedClusterSchedule")
public class PreferSelectedClusterScheduleStrategy extends AbstractClusterScheduler {

    @Override
    public String selectCluster(PipelineTemplate template) {
        List<ClusterScoreCalculator.ClusterScore> available = scoreAvailableClusters(templateCodeOf(template));
        if (available.isEmpty()) {
            throw noAvailableCluster();
        }

        List<String> preferredNames = resolveTemplateClusterNames(template);
        List<ClusterScoreCalculator.ClusterScore> preferredPool = available.stream()
                .filter(s -> preferredNames.contains(s.getClusterName()))
                .collect(Collectors.toList());

        String selected;
        if (!preferredPool.isEmpty()) {
            selected = pickHighest(preferredPool);
            log.info("PreferSelected 策略选中集群（优先池）, templateCode={}, selected={}, preferred={}",
                    templateCodeOf(template), selected, preferredNames);
        } else {
            selected = pickHighest(available);
            log.info("PreferSelected 策略选中集群（兜底溢出，优先池不可用）, templateCode={}, selected={}, preferred={}",
                    templateCodeOf(template), selected, preferredNames);
        }
        return selected;
    }
}
