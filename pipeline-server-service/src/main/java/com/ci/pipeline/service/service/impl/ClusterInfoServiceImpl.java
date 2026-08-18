package com.ci.pipeline.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ci.pipeline.common.auth.UserContext;
import com.ci.pipeline.common.constants.ClusterConstants;
import com.ci.pipeline.common.constants.CommonConstants;
import com.ci.pipeline.common.exception.BusinessException;
import com.ci.pipeline.dao.entity.ClusterInfo;
import com.ci.pipeline.dao.repository.ClusterInfoRepository;
import com.ci.pipeline.dao.repository.PipelineRunRepository;
import com.ci.pipeline.facade.request.ClusterInfoCreateRequest;
import com.ci.pipeline.facade.request.ClusterInfoQueryRequest;
import com.ci.pipeline.facade.request.ClusterInfoUpdateRequest;
import com.ci.pipeline.facade.request.ClusterTestConnectionRequest;
import com.ci.pipeline.facade.response.ClusterInfoResponse;
import com.ci.pipeline.facade.response.ClusterTestConnectionResponse;
import com.ci.pipeline.facade.response.PageResponse;
import com.ci.pipeline.service.service.ClusterInfoService;
import com.ci.pipeline.service.service.ClusterTemplateSyncService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 集群管理服务实现
 */
@Slf4j
@Service
public class ClusterInfoServiceImpl implements ClusterInfoService {

    private static final Pattern CLUSTER_NAME = Pattern.compile(ClusterConstants.CLUSTER_NAME_PATTERN);

    @Autowired
    private ClusterInfoRepository clusterInfoRepository;

    @Autowired
    private PipelineRunRepository pipelineRunRepository;

    @Autowired
    private ClusterTemplateSyncService clusterTemplateSyncService;

    @Override
    public PageResponse<ClusterInfoResponse> page(ClusterInfoQueryRequest query) {
        long pageNum = query.getPageNum() == null ? CommonConstants.DEFAULT_PAGE_NUM : query.getPageNum();
        long pageSize = query.getPageSize() == null ? CommonConstants.DEFAULT_PAGE_SIZE : query.getPageSize();

        LambdaQueryWrapper<ClusterInfo> wrapper = new LambdaQueryWrapper<ClusterInfo>()
                .like(StringUtils.isNotBlank(query.getClusterName()), ClusterInfo::getClusterName, query.getClusterName())
                .eq(query.getEnabled() != null, ClusterInfo::getEnabled, query.getEnabled())
                .eq(query.getOnline() != null, ClusterInfo::getOnline, query.getOnline())
                .orderByAsc(ClusterInfo::getId);
        IPage<ClusterInfo> pageResult = clusterInfoRepository.pageQuery(pageNum, pageSize, wrapper);

        List<ClusterInfoResponse> records = pageResult.getRecords().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return PageResponse.of(records, pageResult.getTotal(), pageResult.getCurrent(),
                pageResult.getSize(), pageResult.getPages());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClusterInfoResponse create(ClusterInfoCreateRequest request) {
        validateClusterName(request.getClusterName());
        validateUrl("argoUrl", request.getArgoUrl());
        validateUrl("k8sMasterUrl", request.getK8sMasterUrl());
        if (StringUtils.isBlank(request.getArgoToken())) {
            throw new BusinessException("argoToken 不能为空");
        }
        if (StringUtils.isBlank(request.getK8sToken())) {
            throw new BusinessException("k8sToken 不能为空");
        }
        if (clusterInfoRepository.selectByClusterName(request.getClusterName()) != null) {
            throw new BusinessException(String.format("集群名已存在, clusterName=%s", request.getClusterName()));
        }

        ClusterInfo entity = new ClusterInfo();
        entity.setClusterName(request.getClusterName());
        entity.setDescription(request.getDescription());
        entity.setArgoUrl(request.getArgoUrl());
        entity.setArgoToken(request.getArgoToken());
        entity.setArgoNamespace(StringUtils.isNotBlank(request.getArgoNamespace())
                ? request.getArgoNamespace() : "argo");
        entity.setK8sMasterUrl(request.getK8sMasterUrl());
        entity.setK8sToken(request.getK8sToken());
        entity.setK8sVerifyingSsl(Boolean.TRUE.equals(request.getK8sVerifyingSsl()) ? 1 : 0);
        entity.setConnectTimeoutMs(nvl(request.getConnectTimeoutMs(), ClusterConstants.DEFAULT_CONNECT_TIMEOUT_MS));
        entity.setReadTimeoutMs(nvl(request.getReadTimeoutMs(), ClusterConstants.DEFAULT_READ_TIMEOUT_MS));
        entity.setFreeMemoryThreshold(request.getFreeMemoryThreshold());
        entity.setMaxRunningWorkflows(request.getMaxRunningWorkflows());
        entity.setEnabled(Boolean.FALSE.equals(request.getEnabled()) ? 0 : 1);
        entity.setOnline(Boolean.FALSE.equals(request.getOnline()) ? 0 : 1);
        // 创建人取当前登录用户（Controller 已 @RequireLogin，保证非空）
        entity.setCreator(UserContext.getUserId());

        boolean setDefault = Boolean.TRUE.equals(request.getIsDefault());
        if (setDefault) {
            clusterInfoRepository.clearDefaultMark();
        }
        entity.setIsDefault(setDefault ? 1 : 0);
        clusterInfoRepository.insert(entity);
        log.info("新增集群成功, clusterName={}, isDefault={}", entity.getClusterName(), setDefault);

        // 异步全量同步已有模板到新集群（默认开启）
        if (Boolean.FALSE.equals(request.getAutoSyncTemplates())) {
            log.info("跳过模板自动同步, clusterName={}", entity.getClusterName());
        } else {
            clusterTemplateSyncService.syncAllTemplatesToClusterAsync(entity.getClusterName());
        }
        return toResponse(clusterInfoRepository.selectById(entity.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClusterInfoResponse update(ClusterInfoUpdateRequest request) {
        if (request.getId() == null) {
            throw new BusinessException("id 不能为空");
        }
        ClusterInfo existing = clusterInfoRepository.selectById(request.getId());
        if (existing == null) {
            throw new BusinessException(String.format("集群不存在, id=%s", request.getId()));
        }
        if (StringUtils.isNotBlank(request.getArgoUrl())) {
            validateUrl("argoUrl", request.getArgoUrl());
            existing.setArgoUrl(request.getArgoUrl());
        }
        if (StringUtils.isNotBlank(request.getK8sMasterUrl())) {
            validateUrl("k8sMasterUrl", request.getK8sMasterUrl());
            existing.setK8sMasterUrl(request.getK8sMasterUrl());
        }
        // token 留空表示不修改
        if (StringUtils.isNotBlank(request.getArgoToken())) {
            existing.setArgoToken(request.getArgoToken());
        }
        if (StringUtils.isNotBlank(request.getK8sToken())) {
            existing.setK8sToken(request.getK8sToken());
        }
        if (StringUtils.isNotBlank(request.getArgoNamespace())) {
            existing.setArgoNamespace(request.getArgoNamespace());
        }
        if (request.getK8sVerifyingSsl() != null) {
            existing.setK8sVerifyingSsl(request.getK8sVerifyingSsl() ? 1 : 0);
        }
        if (request.getConnectTimeoutMs() != null) {
            existing.setConnectTimeoutMs(request.getConnectTimeoutMs());
        }
        if (request.getReadTimeoutMs() != null) {
            existing.setReadTimeoutMs(request.getReadTimeoutMs());
        }
        if (request.getFreeMemoryThreshold() != null) {
            existing.setFreeMemoryThreshold(request.getFreeMemoryThreshold());
        }
        if (request.getMaxRunningWorkflows() != null) {
            existing.setMaxRunningWorkflows(request.getMaxRunningWorkflows());
        }
        if (request.getEnabled() != null) {
            existing.setEnabled(request.getEnabled() ? 1 : 0);
        }
        if (request.getOnline() != null) {
            existing.setOnline(request.getOnline() ? 1 : 0);
        }
        if (request.getDescription() != null) {
            existing.setDescription(request.getDescription());
        }
        // 修改人取当前登录用户（Controller 已 @RequireLogin，保证非空）
        existing.setUpdater(UserContext.getUserId());

        boolean setDefault = Boolean.TRUE.equals(request.getIsDefault());
        if (setDefault && !Integer.valueOf(1).equals(existing.getIsDefault())) {
            clusterInfoRepository.clearDefaultMark();
        }
        if (request.getIsDefault() != null) {
            existing.setIsDefault(setDefault ? 1 : 0);
        }
        clusterInfoRepository.updateById(existing);
        log.info("更新集群成功, clusterName={}", existing.getClusterName());
        return toResponse(clusterInfoRepository.selectById(existing.getId()));
    }

    @Override
    public void delete(Long id) {
        ClusterInfo existing = clusterInfoRepository.selectById(id);
        if (existing == null) {
            throw new BusinessException(String.format("集群不存在, id=%s", id));
        }
        long runCount = pipelineRunRepository.countByClusterName(existing.getClusterName());
        if (runCount > 0) {
            throw new BusinessException(String.format(
                    "该集群存在 %d 条执行记录，无法删除（历史记录需按集群路由日志/详情），请改用「下线」（enabled=0）",
                    runCount));
        }
        clusterInfoRepository.deleteById(id);
        log.info("删除集群成功, clusterName={}", existing.getClusterName());
    }

    @Override
    public void toggleOnline(String clusterName, boolean online) {
        ClusterInfo existing = clusterInfoRepository.selectByClusterName(clusterName);
        if (existing == null) {
            throw new BusinessException(String.format("集群不存在, clusterName=%s", clusterName));
        }
        existing.setOnline(online ? 1 : 0);
        clusterInfoRepository.updateById(existing);
        log.info("集群摘流状态变更, clusterName={}, online={}", clusterName, online);
    }

    @Override
    public ClusterTestConnectionResponse testConnection(ClusterTestConnectionRequest request) {
        validateUrl("argoUrl", request.getArgoUrl());
        validateUrl("k8sMasterUrl", request.getK8sMasterUrl());

        // 编辑场景：表单 token 留空时复用库中 token
        String argoToken = request.getArgoToken();
        String k8sToken = request.getK8sToken();
        boolean verifyingSsl = Boolean.TRUE.equals(request.getK8sVerifyingSsl());
        if (request.getId() != null && StringUtils.isBlank(argoToken) && StringUtils.isBlank(k8sToken)) {
            ClusterInfo saved = clusterInfoRepository.selectById(request.getId());
            if (saved != null) {
                argoToken = saved.getArgoToken();
                k8sToken = saved.getK8sToken();
                verifyingSsl = saved.getK8sVerifyingSsl() != null && saved.getK8sVerifyingSsl() == 1;
            }
        }
        if (StringUtils.isBlank(argoToken) || StringUtils.isBlank(k8sToken)) {
            throw new BusinessException("token 不能为空");
        }

        ClusterTestConnectionResponse response = new ClusterTestConnectionResponse();
        int connectTimeout = nvl(request.getConnectTimeoutMs(), ClusterConstants.DEFAULT_CONNECT_TIMEOUT_MS);
        int readTimeout = nvl(request.getReadTimeoutMs(), ClusterConstants.DEFAULT_READ_TIMEOUT_MS);

        // Argo 探测：GET /api/v1/info（Argo Server 多为自签证书，与 ClusterClientRegistry.buildArgoClient 一致按 trust-all 处理，
        // 否则探测结果与实际运行时行为不一致：运行时能连上、探测却报 PKIX 握手失败）
        long argoStart = System.currentTimeMillis();
        try {
            javax.net.ssl.TrustManager[] trustAll = trustAllCerts();
            javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("TLS");
            sslContext.init(null, trustAll, new java.security.SecureRandom());
            okhttp3.OkHttpClient client = new okhttp3.OkHttpClient.Builder()
                    .connectTimeout(java.time.Duration.ofMillis(connectTimeout))
                    .readTimeout(java.time.Duration.ofMillis(readTimeout))
                    .sslSocketFactory(sslContext.getSocketFactory(), (javax.net.ssl.X509TrustManager) trustAll[0])
                    .hostnameVerifier((hostname, session) -> true)
                    .build();
            okhttp3.Request argoReq = new okhttp3.Request.Builder()
                    .url(request.getArgoUrl().replaceAll("/+$", "") + "/api/v1/info")
                    .header("Authorization", argoToken.startsWith("Bearer") ? argoToken : "Bearer " + argoToken)
                    .build();
            try (okhttp3.Response argoResp = client.newCall(argoReq).execute()) {
                response.setArgoOk(argoResp.isSuccessful());
                response.setArgoMessage(argoResp.isSuccessful()
                        ? "连接成功"
                        : "HTTP " + argoResp.code());
            }
        } catch (Exception e) {
            response.setArgoOk(false);
            response.setArgoMessage(e.getMessage());
        }
        response.setArgoCostMs(System.currentTimeMillis() - argoStart);

        // K8s 探测：listNode limit=1（临时客户端，不进注册表）
        long k8sStart = System.currentTimeMillis();
        try {
            io.kubernetes.client.openapi.ApiClient apiClient = new io.kubernetes.client.openapi.ApiClient();
            apiClient.setBasePath(request.getK8sMasterUrl());
            apiClient.setApiKey(k8sToken);
            apiClient.setApiKeyPrefix("Bearer");
            apiClient.setVerifyingSsl(verifyingSsl);
            apiClient.setConnectTimeout(connectTimeout);
            apiClient.setReadTimeout(readTimeout);
            io.kubernetes.client.openapi.apis.CoreV1Api coreV1Api = new io.kubernetes.client.openapi.apis.CoreV1Api(apiClient);
            coreV1Api.listNode("1", null, null, null, null, null, null, null, null, null);
            response.setK8sOk(true);
            response.setK8sMessage("连接成功");
        } catch (io.kubernetes.client.openapi.ApiException e) {
            // 403 多为 token RBAC 不足（如误用 argo-server SA token 调 listNode），回显响应体便于定位
            response.setK8sOk(false);
            response.setK8sMessage("HTTP " + e.getCode()
                    + (StringUtils.isNotBlank(e.getResponseBody()) ? " " + e.getResponseBody() : ""));
        } catch (Exception e) {
            response.setK8sOk(false);
            response.setK8sMessage(e.getMessage());
        }
        response.setK8sCostMs(System.currentTimeMillis() - k8sStart);
        response.setAllOk(Boolean.TRUE.equals(response.getArgoOk()) && Boolean.TRUE.equals(response.getK8sOk()));
        return response;
    }

    private void validateClusterName(String clusterName) {
        if (StringUtils.isBlank(clusterName) || !CLUSTER_NAME.matcher(clusterName).matches()) {
            throw new BusinessException("集群名不合法（小写字母数字中划线，长度 1~100）");
        }
    }

    private void validateUrl(String fieldName, String url) {
        if (StringUtils.isBlank(url)) {
            throw new BusinessException(String.format("%s 不能为空", fieldName));
        }
        if (!(url.startsWith("http://") || url.startsWith("https://"))) {
            throw new BusinessException(String.format("%s 必须以 http:// 或 https:// 开头", fieldName));
        }
    }

    private ClusterInfoResponse toResponse(ClusterInfo entity) {
        ClusterInfoResponse response = new ClusterInfoResponse();
        response.setId(entity.getId());
        response.setClusterName(entity.getClusterName());
        response.setDescription(entity.getDescription());
        response.setArgoUrl(entity.getArgoUrl());
        response.setArgoTokenMasked(maskToken(entity.getArgoToken()));
        response.setArgoNamespace(entity.getArgoNamespace());
        response.setK8sMasterUrl(entity.getK8sMasterUrl());
        response.setK8sTokenMasked(maskToken(entity.getK8sToken()));
        response.setK8sVerifyingSsl(entity.getK8sVerifyingSsl() != null && entity.getK8sVerifyingSsl() == 1);
        response.setConnectTimeoutMs(entity.getConnectTimeoutMs());
        response.setReadTimeoutMs(entity.getReadTimeoutMs());
        response.setFreeMemoryThreshold(entity.getFreeMemoryThreshold());
        response.setMaxRunningWorkflows(entity.getMaxRunningWorkflows());
        response.setEnabled(entity.getEnabled() != null && entity.getEnabled() == 1);
        response.setOnline(entity.getOnline() != null && entity.getOnline() == 1);
        response.setIsDefault(entity.getIsDefault() != null && entity.getIsDefault() == 1);
        response.setCreator(entity.getCreator());
        response.setCreateTime(entity.getCreateTime());
        response.setUpdater(entity.getUpdater());
        response.setUpdateTime(entity.getUpdateTime());
        return response;
    }

    /**
     * token 脱敏：只回显后 4 位
     */
    private String maskToken(String token) {
        if (StringUtils.isBlank(token)) {
            return "";
        }
        String bare = token.startsWith("Bearer ") ? token.substring(7) : token;
        if (bare.length() <= 4) {
            return "****";
        }
        return "****" + bare.substring(bare.length() - 4);
    }

    private int nvl(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    /**
     * trust-all TrustManager（自签证书探测用，与 ClusterClientRegistry 行为一致）
     */
    private javax.net.ssl.TrustManager[] trustAllCerts() {
        return new javax.net.ssl.TrustManager[]{
                new javax.net.ssl.X509TrustManager() {
                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[0];
                    }
                }
        };
    }
}
