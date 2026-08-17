package com.ci.pipeline.service.service.impl;

import com.ci.pipeline.common.auth.UserContext;
import com.ci.pipeline.common.constants.DistributedLockConstants;
import com.ci.pipeline.common.constants.PipelineTemplateConstants;
import com.ci.pipeline.common.enums.PipelineTemplateVersionStatusEnum;
import com.ci.pipeline.common.exception.BusinessException;
import com.ci.pipeline.dao.entity.PipelineTemplateVersion;
import com.ci.pipeline.dao.repository.PipelineParameterRepository;
import com.ci.pipeline.dao.repository.PipelineTemplateRepository;
import com.ci.pipeline.dao.repository.PipelineTemplateVersionRepository;
import com.ci.pipeline.facade.request.PipelineTemplateVersionCreateRequest;
import com.ci.pipeline.facade.request.PipelineTemplateVersionStatusRequest;
import com.ci.pipeline.facade.request.PipelineTemplateVersionUpdateRequest;
import com.ci.pipeline.facade.response.PipelineTemplateVersionResponse;
import com.ci.pipeline.facade.response.PipelineTemplateVersionSaveResponse;
import com.ci.pipeline.service.service.ClusterTemplateSyncService;
import com.ci.pipeline.service.service.DistributedLockService;
import com.ci.pipeline.service.service.PipelineTemplateVersionService;
import com.ci.pipeline.service.util.ArgoWorkflowUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.argoproj.workflow.models.IoArgoprojWorkflowV1alpha1WorkflowTemplate;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 流水线模板版本业务实现
 */
@Slf4j
@Service
public class PipelineTemplateVersionServiceImpl implements PipelineTemplateVersionService {

    /**
     * 版本号格式：三段点分数字，每段无前导零（如 0.0.1、1.10.3）
     */
    private static final Pattern VERSION_PATTERN = Pattern.compile("^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)$");

    @Autowired
    private PipelineTemplateVersionRepository pipelineTemplateVersionRepository;

    @Autowired
    private PipelineTemplateRepository pipelineTemplateRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClusterTemplateSyncService clusterTemplateSyncService;

    @Autowired
    private PipelineParameterRepository pipelineParameterRepository;

    @Autowired
    private DistributedLockService distributedLockService;

    @Override
    public PipelineTemplateVersionSaveResponse create(PipelineTemplateVersionCreateRequest request) {
        // ① 流水线模板必须存在
        if (!StringUtils.hasText(request.getPipelineTemplateCode())) {
            throw new BusinessException(PipelineTemplateConstants.MSG_TEMPLATE_CODE_REQUIRED);
        }
        if (pipelineTemplateRepository.selectByPipelineTemplateCode(request.getPipelineTemplateCode()) == null) {
            throw new BusinessException(PipelineTemplateConstants.MSG_TEMPLATE_NOT_EXIST);
        }
        // 非阻塞加锁，防止并发或连击
        String lockKey = DistributedLockConstants.LOCK_KEY_PIPELINE_TEMPLATE + request.getPipelineTemplateCode();
        String lockValue = distributedLockService.tryLock(
                lockKey, DistributedLockConstants.DEFAULT_LOCK_EXPIRE_SECONDS, "新增流水线模板版本");
        if (lockValue == null) {
            throw new BusinessException(PipelineTemplateConstants.MSG_OPERATION_LOCK_FAILED);
        }
        try {
            // ② 模板详情必填，且需能解析为合法的 Argo WorkflowTemplate
            if (!StringUtils.hasText(request.getTemplateDetail())) {
                throw new BusinessException(PipelineTemplateConstants.MSG_TEMPLATE_DETAIL_REQUIRED);
            }
            IoArgoprojWorkflowV1alpha1WorkflowTemplate workflowTemplate = parseWorkflowTemplate(request.getTemplateDetail());
            // 校验模板详情 metadata.name 与流水线模板编码一致
            validateTemplateNameMatchCode(workflowTemplate, request.getPipelineTemplateCode());
            // 校验模板参数是否都在参数定义表中配置，未定义则直接返回（不保存）
            List<String> undefined = findUndefinedParams(request.getTemplateDetail());
            if (!undefined.isEmpty()) {
                return PipelineTemplateVersionSaveResponse.undefined(undefined);
            }
            // ③ 版本号格式校验
            if (!StringUtils.hasText(request.getVersion())) {
                throw new BusinessException(PipelineTemplateConstants.MSG_VERSION_REQUIRED);
            }
            int[] newVer = parseVersion(request.getVersion());
            // ④ (pipeline_template_code, version) 唯一性
            if (pipelineTemplateVersionRepository.selectByCodeAndVersion(
                    request.getPipelineTemplateCode(), request.getVersion()) != null) {
                throw new BusinessException(String.format(
                        PipelineTemplateConstants.MSG_VERSION_DUPLICATED,
                        request.getPipelineTemplateCode(), request.getVersion()));
            }
            // ⑤ 版本递增规则校验（只能递增不能递减，相对当前最大版本）
            validateIncrement(request.getPipelineTemplateCode(), newVer, request.getVersion());

            PipelineTemplateVersion entity = new PipelineTemplateVersion();
            BeanUtils.copyProperties(request, entity);
            entity.setCreator(UserContext.getUserId());
            // 新版本默认草稿
            entity.setStatus(PipelineTemplateVersionStatusEnum.DRAFT.getCode());
            pipelineTemplateVersionRepository.insert(entity);
            log.info("新增流水线模板版本成功, pipelineTemplateCode={}, version={}, id={}",
                    entity.getPipelineTemplateCode(), entity.getVersion(), entity.getId());
            return PipelineTemplateVersionSaveResponse.ok(
                    toResponse(pipelineTemplateVersionRepository.selectById(entity.getId())));
        } finally {
            distributedLockService.unlock(lockKey, lockValue);
        }
    }

    @Override
    public PipelineTemplateVersionSaveResponse update(PipelineTemplateVersionUpdateRequest request) {
        requireCodeAndVersion(request.getPipelineTemplateCode(), request.getVersion());
        // 非阻塞加锁，防止并发或连击
        String lockKey = DistributedLockConstants.LOCK_KEY_PIPELINE_TEMPLATE + request.getPipelineTemplateCode();
        String lockValue = distributedLockService.tryLock(
                lockKey, DistributedLockConstants.DEFAULT_LOCK_EXPIRE_SECONDS, "修改流水线模板版本");
        if (lockValue == null) {
            throw new BusinessException(PipelineTemplateConstants.MSG_OPERATION_LOCK_FAILED);
        }
        try {
            PipelineTemplateVersion existing = pipelineTemplateVersionRepository.selectByCodeAndVersion(
                    request.getPipelineTemplateCode(), request.getVersion());
            if (existing == null) {
                throw new BusinessException(PipelineTemplateConstants.MSG_VERSION_NOT_EXIST);
            }
            // 仅草稿状态的版本允许修改（生效中 / 已失效需通过「新增版本」走版本变更）
            if (!PipelineTemplateVersionStatusEnum.DRAFT.getCode().equals(existing.getStatus())) {
                throw new BusinessException(String.format(
                        PipelineTemplateConstants.MSG_VERSION_UPDATE_STATUS_INVALID, existing.getStatus()));
            }
            // 模板详情必填，且需能解析为合法的 Argo WorkflowTemplate
            if (!StringUtils.hasText(request.getTemplateDetail())) {
                throw new BusinessException(PipelineTemplateConstants.MSG_TEMPLATE_DETAIL_REQUIRED);
            }
            IoArgoprojWorkflowV1alpha1WorkflowTemplate workflowTemplate = parseWorkflowTemplate(request.getTemplateDetail());
            // 校验模板详情 metadata.name 与流水线模板编码一致
            validateTemplateNameMatchCode(workflowTemplate, request.getPipelineTemplateCode());
            // 校验模板参数是否都在参数定义表中配置，未定义则直接返回（不保存）
            List<String> undefined = findUndefinedParams(request.getTemplateDetail());
            if (!undefined.isEmpty()) {
                return PipelineTemplateVersionSaveResponse.undefined(undefined);
            }
            // 仅允许修改 templateDetail / changeNote，version 为业务键不可改、status 走状态变更接口
            PipelineTemplateVersion entity = new PipelineTemplateVersion();
            entity.setId(existing.getId());
            entity.setTemplateDetail(request.getTemplateDetail());
            entity.setChangeNote(request.getChangeNote());
            pipelineTemplateVersionRepository.updateById(entity);
            log.info("修改流水线模板版本成功, pipelineTemplateCode={}, version={}",
                    request.getPipelineTemplateCode(), request.getVersion());
            return PipelineTemplateVersionSaveResponse.ok(toResponse(
                    pipelineTemplateVersionRepository.selectByCodeAndVersion(
                            request.getPipelineTemplateCode(), request.getVersion())));
        } finally {
            distributedLockService.unlock(lockKey, lockValue);
        }
    }

    @Override
    public void deleteById(Long id) {
        PipelineTemplateVersion existing = pipelineTemplateVersionRepository.selectById(id);
        if (existing == null) {
            throw new BusinessException(PipelineTemplateConstants.MSG_VERSION_NOT_EXIST);
        }
        // 落库前先打通 argo：按名称删除所有 enabled 集群上对应的 WorkflowTemplate
        IoArgoprojWorkflowV1alpha1WorkflowTemplate workflowTemplate = parseWorkflowTemplate(existing.getTemplateDetail());
        String templateName = getWorkflowTemplateName(workflowTemplate);
        List<com.ci.pipeline.facade.response.ClusterSyncResultResponse> deleteResults =
                clusterTemplateSyncService.deleteTemplateFromAllClusters(existing.getPipelineTemplateCode(), templateName);
        List<String> failures = deleteResults.stream()
                .filter(r -> !Boolean.TRUE.equals(r.getSuccess()))
                .map(r -> r.getClusterName() + ": " + r.getErrorMessage())
                .collect(java.util.stream.Collectors.toList());
        if (!failures.isEmpty()) {
            throw new BusinessException(String.format(
                    "部分集群删除模板失败，本次未删除记录，请处理后重试: %s", String.join("; ", failures)));
        }
        pipelineTemplateVersionRepository.deleteById(id);
        log.info("删除流水线模板版本成功, id={}, pipelineTemplateCode={}, version={}",
                id, existing.getPipelineTemplateCode(), existing.getVersion());
    }

    @Override
    public PipelineTemplateVersionResponse getDetail(String pipelineTemplateCode, String version) {
        requireCodeAndVersion(pipelineTemplateCode, version);
        PipelineTemplateVersion entity = pipelineTemplateVersionRepository.selectByCodeAndVersion(
                pipelineTemplateCode, version);
        if (entity == null) {
            throw new BusinessException(PipelineTemplateConstants.MSG_VERSION_NOT_EXIST);
        }
        return toResponse(entity);
    }

    @Override
    public List<PipelineTemplateVersionResponse> listByCode(String pipelineTemplateCode) {
        if (!StringUtils.hasText(pipelineTemplateCode)) {
            throw new BusinessException(PipelineTemplateConstants.MSG_TEMPLATE_CODE_REQUIRED);
        }
        List<PipelineTemplateVersion> versions = pipelineTemplateVersionRepository.listByCode(pipelineTemplateCode);
        // 按创建时间倒序（代码内排序，非 SQL），列表项不返回模板详情（减少传输）
        return versions.stream()
                .sorted(Comparator.comparing(PipelineTemplateVersion::getCreateTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toListResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PipelineTemplateVersionResponse changeStatus(PipelineTemplateVersionStatusRequest request) {
        if (request == null || !StringUtils.hasText(request.getStatus())) {
            throw new BusinessException(String.format(
                    PipelineTemplateConstants.MSG_VERSION_STATUS_INVALID, request == null ? null : request.getStatus()));
        }
        requireCodeAndVersion(request.getPipelineTemplateCode(), request.getVersion());
        // 目标状态必须是合法枚举
        if (!PipelineTemplateVersionStatusEnum.isValidCode(request.getStatus())) {
            throw new BusinessException(String.format(
                    PipelineTemplateConstants.MSG_VERSION_STATUS_INVALID, request.getStatus()));
        }
        // 非阻塞加锁，防止并发或连击
        String lockKey = DistributedLockConstants.LOCK_KEY_PIPELINE_TEMPLATE + request.getPipelineTemplateCode();
        String lockValue = distributedLockService.tryLock(
                lockKey, DistributedLockConstants.DEFAULT_LOCK_EXPIRE_SECONDS, "发布流水线模板版本");
        if (lockValue == null) {
            throw new BusinessException(PipelineTemplateConstants.MSG_OPERATION_LOCK_FAILED);
        }
        try {
            PipelineTemplateVersion existing = pipelineTemplateVersionRepository.selectByCodeAndVersion(
                    request.getPipelineTemplateCode(), request.getVersion());
            if (existing == null) {
                throw new BusinessException(PipelineTemplateConstants.MSG_VERSION_NOT_EXIST);
            }

            // 幂等：目标状态与当前状态一致时直接返回，不重复触发副作用（如失效其它版本）
            if (existing.getStatus().equals(request.getStatus())) {
                log.info("流水线模板版本状态未变化，跳过更新（幂等）, pipelineTemplateCode={}, version={}, status={}",
                        request.getPipelineTemplateCode(), request.getVersion(), request.getStatus());
                return toResponse(existing);
            }

            // 发布（目标为生效中）：落库前先打通 argo——不存在则创建，存在则更新（所有 enabled 集群）；
            // 并校验模板详情 metadata.name 与流水线模板编码一致
            if (PipelineTemplateVersionStatusEnum.EFFECTIVE.getCode().equals(request.getStatus())) {
                IoArgoprojWorkflowV1alpha1WorkflowTemplate workflowTemplate =
                        parseWorkflowTemplate(existing.getTemplateDetail());
                validateTemplateNameMatchCode(workflowTemplate, request.getPipelineTemplateCode());
                List<com.ci.pipeline.facade.response.ClusterSyncResultResponse> syncResults =
                        clusterTemplateSyncService.saveTemplateToAllClusters(
                                request.getPipelineTemplateCode(), existing.getTemplateDetail());
                List<String> failures = syncResults.stream()
                        .filter(r -> !Boolean.TRUE.equals(r.getSuccess()))
                        .map(r -> r.getClusterName() + ": " + r.getErrorMessage())
                        .collect(java.util.stream.Collectors.toList());
                if (!failures.isEmpty()) {
                    // 部分集群失败：DB 状态照常变更（多数集群已成功，回滚反而造成更大不一致），
                    // 日志记录失败明细，可通过模板重推接口补偿
                    log.warn("流水线模板发布部分集群同步失败, pipelineTemplateCode={}, failures={}",
                            request.getPipelineTemplateCode(), failures);
                }
            }

            // 目标为生效中时，其它尚未失效的版本（生效中 / 草稿）统一自动置为已失效
            if (PipelineTemplateVersionStatusEnum.EFFECTIVE.getCode().equals(request.getStatus())) {
                pipelineTemplateVersionRepository.updateOtherStatusToExpired(
                        request.getPipelineTemplateCode(), request.getVersion());
            }
            pipelineTemplateVersionRepository.updateStatusByCodeAndVersion(
                    request.getPipelineTemplateCode(), request.getVersion(), request.getStatus());
            log.info("变更流水线模板版本状态成功, pipelineTemplateCode={}, version={}, status={}",
                    request.getPipelineTemplateCode(), request.getVersion(), request.getStatus());
            return toResponse(pipelineTemplateVersionRepository.selectByCodeAndVersion(
                    request.getPipelineTemplateCode(), request.getVersion()));
        } finally {
            distributedLockService.unlock(lockKey, lockValue);
        }
    }

    // ===== 私有工具方法 =====

    /**
     * 校验 pipeline_template_code 与 version 非空
     */
    private void requireCodeAndVersion(String pipelineTemplateCode, String version) {
        if (!StringUtils.hasText(pipelineTemplateCode)) {
            throw new BusinessException(PipelineTemplateConstants.MSG_TEMPLATE_CODE_REQUIRED);
        }
        if (!StringUtils.hasText(version)) {
            throw new BusinessException(PipelineTemplateConstants.MSG_VERSION_REQUIRED);
        }
    }

    /**
     * 解析版本号为三段整数，格式非法抛业务异常
     */
    private int[] parseVersion(String version) {
        if (!VERSION_PATTERN.matcher(version).matches()) {
            throw new BusinessException(PipelineTemplateConstants.MSG_VERSION_FORMAT_INVALID);
        }
        String[] parts = version.split("\\.");
        int[] tuple = new int[3];
        for (int i = 0; i < 3; i++) {
            tuple[i] = Integer.parseInt(parts[i]);
        }
        return tuple;
    }

    /**
     * 版本递增规则校验（相对当前最大版本）：版本号只能递增不能递减。
     * <ul>
     *     <li>无历史版本时（首个版本）：格式合法即可；</li>
     *     <li>有历史版本时：传入版本必须严格大于当前最大版本。</li>
     * </ul>
     *
     * @param pipelineTemplateCode 流水线模板编码
     * @param newVer               传入版本的三段整数
     * @param rawVersion           传入版本原始字符串（用于错误提示）
     */
    private void validateIncrement(String pipelineTemplateCode, int[] newVer, String rawVersion) {
        List<String> existing = pipelineTemplateVersionRepository.listVersionsByCode(pipelineTemplateCode);
        if (existing.isEmpty()) {
            return;
        }
        // 求当前最大版本
        int[] max = null;
        String maxRaw = null;
        for (String v : existing) {
            int[] parsed = parseVersion(v);
            if (max == null || compare(parsed, max) > 0) {
                max = parsed;
                maxRaw = v;
            }
        }
        // 整体必须严格更大
        if (compare(newVer, max) <= 0) {
            throw new BusinessException(String.format(
                    PipelineTemplateConstants.MSG_VERSION_INCREMENT_INVALID, maxRaw, rawVersion));
        }
    }

    /**
     * 三段版本号字典序比较
     *
     * @return 负数 / 0 / 正数
     */
    private int compare(int[] a, int[] b) {
        for (int i = 0; i < 3; i++) {
            if (a[i] != b[i]) {
                return Integer.compare(a[i], b[i]);
            }
        }
        return 0;
    }

    /**
     * 将 templateDetail 解析为 {@link IoArgoprojWorkflowV1alpha1WorkflowTemplate}，
     * 无法解析时抛业务异常提示（同时承担了原模板详情合法性校验的职责）。
     */
    private IoArgoprojWorkflowV1alpha1WorkflowTemplate parseWorkflowTemplate(String templateDetail) {
        try {
            return objectMapper.readValue(templateDetail, IoArgoprojWorkflowV1alpha1WorkflowTemplate.class);
        } catch (Exception e) {
            throw new BusinessException(String.format(
                    PipelineTemplateConstants.MSG_TEMPLATE_DETAIL_INVALID, e.getMessage()));
        }
    }

    /**
     * 校验 WorkflowTemplate 的 metadata.name 与流水线模板编码一致，不一致抛业务异常。
     * <p>argo WorkflowTemplate 以流水线模板编码命名，所有版本共享同一个 argo 模板，
     * 故模板详情中的 name 必须与所属流水线模板编码严格相等。
     */
    private void validateTemplateNameMatchCode(IoArgoprojWorkflowV1alpha1WorkflowTemplate workflowTemplate,
                                               String pipelineTemplateCode) {
        String name = getWorkflowTemplateName(workflowTemplate);
        if (!pipelineTemplateCode.equals(name)) {
            throw new BusinessException(String.format(
                    PipelineTemplateConstants.MSG_TEMPLATE_NAME_NOT_MATCH_CODE, name, pipelineTemplateCode));
        }
    }

    /**
     * 校验模板详情中的输入参数是否都在参数定义表中配置。
     * <p>解析 templateDetail 的 spec.arguments.parameters，提取所有参数名，
     * 逐个校验是否存在于 pipeline_parameter 表中。未配置的参数名收集后抛业务异常。
     *
     * @param templateDetail WorkflowTemplate 的 json 字符串
     */
    /**
     * 校验模板参数是否都在参数定义表中配置，返回未定义的参数名列表。
     *
     * @param templateDetail WorkflowTemplate 的 json 字符串
     * @return 未定义参数名列表，空列表表示全部已定义
     */
    private List<String> findUndefinedParams(String templateDetail) {
        List<String> paramNames = ArgoWorkflowUtil.extractParamNames(templateDetail, objectMapper);
        if (paramNames.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> existingNames = pipelineParameterRepository.listExistingNames(paramNames);
        Set<String> existingSet = new HashSet<>(existingNames);
        List<String> undefined = new ArrayList<>();
        for (String name : paramNames) {
            if (!existingSet.contains(name)) {
                undefined.add(name);
            }
        }
        return undefined;
    }

    /**
     * 提取 WorkflowTemplate 的名称（metadata.name），缺失时抛业务异常。
     */
    private String getWorkflowTemplateName(IoArgoprojWorkflowV1alpha1WorkflowTemplate workflowTemplate) {
        String name = Optional.ofNullable(workflowTemplate)
                .map(IoArgoprojWorkflowV1alpha1WorkflowTemplate::getMetadata)
                .map(V1ObjectMeta::getName)
                .orElse(null);
        if (!StringUtils.hasText(name)) {
            throw new BusinessException(PipelineTemplateConstants.MSG_TEMPLATE_METADATA_NAME_REQUIRED);
        }
        return name;
    }

    private PipelineTemplateVersionResponse toResponse(PipelineTemplateVersion entity) {
        if (entity == null) {
            return null;
        }
        PipelineTemplateVersionResponse response = new PipelineTemplateVersionResponse();
        BeanUtils.copyProperties(entity, response);
        return response;
    }

    /**
     * 列表用响应：不携带 templateDetail（配合全局 non_null 序列化省略）
     */
    private PipelineTemplateVersionResponse toListResponse(PipelineTemplateVersion entity) {
        PipelineTemplateVersionResponse response = toResponse(entity);
        response.setTemplateDetail(null);
        return response;
    }
}
