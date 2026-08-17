package com.ci.pipeline.service.scheduler.cluster;

import com.ci.pipeline.dao.entity.PipelineTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Any 策略：任意集群。
 * <p>忽略模板上配置的候选集群（cluster_names），在全部可用集群中选得分最高者。
 */
@Slf4j
@Component("AnyClusterSchedule")
public class AnyClusterScheduleStrategy extends AbstractClusterScheduler {

    @Override
    public String selectCluster(PipelineTemplate template) {
        List<ClusterScoreCalculator.ClusterScore> available = scoreAvailableClusters(templateCodeOf(template));
        if (available.isEmpty()) {
            throw noAvailableCluster();
        }
        String selected = pickHighest(available);
        log.info("Any 策略选中集群, templateCode={}, selected={}", templateCodeOf(template), selected);
        return selected;
    }
}
