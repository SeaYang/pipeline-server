package com.ci.pipeline.service.service.impl;

import com.ci.pipeline.common.constants.ClusterConstants;
import com.ci.pipeline.common.exception.BusinessException;
import com.ci.pipeline.dao.entity.ClusterInfo;
import com.ci.pipeline.dao.repository.ClusterInfoRepository;
import com.ci.pipeline.service.service.ClusterConfigService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 集群配置读取服务实现。
 * <p>cluster_info 是低频变更的配置表，直接查库即可，不做缓存；
 * 客户端实例由 {@link ClusterClientRegistry} 按单集群指纹懒加载、变更自动重建，无需额外通知机制。
 */
@Slf4j
@Service
public class ClusterConfigServiceImpl implements ClusterConfigService {

    @Autowired
    private ClusterInfoRepository clusterInfoRepository;

    @Override
    public List<ClusterInfo> listAll() {
        return clusterInfoRepository.listAll();
    }

    @Override
    public List<ClusterInfo> listEnabled() {
        return listAll().stream()
                .filter(c -> c.getEnabled() != null && c.getEnabled() == 1)
                .collect(Collectors.toList());
    }

    @Override
    public List<ClusterInfo> listSchedulable() {
        return listAll().stream()
                .filter(c -> c.getEnabled() != null && c.getEnabled() == 1)
                .filter(c -> c.getOnline() != null && c.getOnline() == 1)
                .collect(Collectors.toList());
    }

    @Override
    public ClusterInfo getByClusterName(String clusterName) {
        return listAll().stream()
                .filter(c -> c.getClusterName().equals(clusterName))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        String.format("集群不存在, clusterName=%s", clusterName)));
    }

    @Override
    public String getDefaultClusterName() {
        List<ClusterInfo> all = listAll();
        return all.stream()
                .filter(c -> c.getIsDefault() != null && c.getIsDefault() == 1)
                .findFirst()
                .map(ClusterInfo::getClusterName)
                .orElseGet(() -> all.stream()
                        .filter(c -> c.getEnabled() != null && c.getEnabled() == 1)
                        .findFirst()
                        .map(ClusterInfo::getClusterName)
                        .orElseThrow(() -> new BusinessException("当前暂无可用集群，请先在集群管理页面录入集群")));
    }

    @Override
    public String getNamespace(String clusterName) {
        ClusterInfo cluster = getByClusterName(clusterName);
        return StringUtils.isNotBlank(cluster.getArgoNamespace())
                ? cluster.getArgoNamespace()
                : "argo";
    }

    @Override
    public String joinClusterNames(List<String> clusterNames) {
        if (clusterNames == null || clusterNames.isEmpty()) {
            return null;
        }
        return clusterNames.stream()
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .distinct()
                .collect(Collectors.joining(ClusterConstants.CLUSTER_NAMES_SEPARATOR));
    }

    @Override
    public List<String> splitClusterNames(String clusterNames) {
        if (StringUtils.isBlank(clusterNames)) {
            return new ArrayList<>();
        }
        return java.util.Arrays.stream(clusterNames.split(ClusterConstants.CLUSTER_NAMES_SEPARATOR))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public String resolveRunClusterName(com.ci.pipeline.dao.entity.PipelineRun run) {
        if (run != null && StringUtils.isNotBlank(run.getClusterName())) {
            return run.getClusterName();
        }
        return getDefaultClusterName();
    }

    @Override
    public String resolveRunNamespace(com.ci.pipeline.dao.entity.PipelineRun run) {
        return getNamespace(resolveRunClusterName(run));
    }
}
