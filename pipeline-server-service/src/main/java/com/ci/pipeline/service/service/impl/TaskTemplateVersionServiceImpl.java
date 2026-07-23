package com.ci.pipeline.service.service.impl;

import com.ci.pipeline.common.auth.UserContext;
import com.ci.pipeline.common.constants.TaskTemplateConstants;
import com.ci.pipeline.common.enums.TaskTemplateVersionStatusEnum;
import com.ci.pipeline.common.exception.BusinessException;
import com.ci.pipeline.dao.entity.TaskTemplateVersion;
import com.ci.pipeline.dao.repository.TaskTemplateRepository;
import com.ci.pipeline.dao.repository.TaskTemplateVersionRepository;
import com.ci.pipeline.facade.request.TaskTemplateVersionCreateRequest;
import com.ci.pipeline.facade.request.TaskTemplateVersionStatusRequest;
import com.ci.pipeline.facade.request.TaskTemplateVersionUpdateRequest;
import com.ci.pipeline.facade.response.TaskTemplateVersionResponse;
import com.ci.pipeline.service.config.ArgoServerProperties;
import com.ci.pipeline.service.remote.ArgoWorkflowAgent;
import com.ci.pipeline.service.service.TaskTemplateVersionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.argoproj.workflow.models.IoArgoprojWorkflowV1alpha1WorkflowTemplate;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 任务模板版本业务实现
 */
@Slf4j
@Service
public class TaskTemplateVersionServiceImpl implements TaskTemplateVersionService {

    /**
     * 版本号格式：三段点分数字，每段无前导零（如 0.0.1、1.10.3）
     */
    private static final Pattern VERSION_PATTERN = Pattern.compile("^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)$");

    @Autowired
    private TaskTemplateVersionRepository taskTemplateVersionRepository;

    @Autowired
    private TaskTemplateRepository taskTemplateRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ArgoWorkflowAgent argoWorkflowAgent;

    @Autowired
    private ArgoServerProperties argoServerProperties;

    @Override
    public TaskTemplateVersionResponse create(TaskTemplateVersionCreateRequest request) {
        // ① 任务模板必须存在
        if (!StringUtils.hasText(request.getTaskTemplateCode())) {
            throw new BusinessException(TaskTemplateConstants.MSG_TEMPLATE_CODE_REQUIRED);
        }
        if (taskTemplateRepository.selectByTaskTemplateCode(request.getTaskTemplateCode()) == null) {
            throw new BusinessException(TaskTemplateConstants.MSG_TEMPLATE_NOT_EXIST);
        }
        // ② 模板详情必填，且需能解析为合法的 Argo WorkflowTemplate
        if (!StringUtils.hasText(request.getTemplateDetail())) {
            throw new BusinessException(TaskTemplateConstants.MSG_TEMPLATE_DETAIL_REQUIRED);
        }
        IoArgoprojWorkflowV1alpha1WorkflowTemplate workflowTemplate = parseWorkflowTemplate(request.getTemplateDetail());
        // 校验模板详情 metadata.name 与任务模板编码一致
        validateTemplateNameMatchCode(workflowTemplate, request.getTaskTemplateCode());
        // ③ 版本号格式校验
        if (!StringUtils.hasText(request.getVersion())) {
            throw new BusinessException(TaskTemplateConstants.MSG_VERSION_REQUIRED);
        }
        int[] newVer = parseVersion(request.getVersion());
        // ④ (task_template_code, version) 唯一性
        if (taskTemplateVersionRepository.selectByCodeAndVersion(
                request.getTaskTemplateCode(), request.getVersion()) != null) {
            throw new BusinessException(String.format(
                    TaskTemplateConstants.MSG_VERSION_DUPLICATED, request.getTaskTemplateCode(), request.getVersion()));
        }
        // ⑤ 版本递增规则校验（相对当前最大版本）
        validateIncrement(request.getTaskTemplateCode(), newVer, request.getVersion());

        TaskTemplateVersion entity = new TaskTemplateVersion();
        BeanUtils.copyProperties(request, entity);
        entity.setCreator(UserContext.getUserId());
        // 新版本默认草稿
        entity.setStatus(TaskTemplateVersionStatusEnum.DRAFT.getCode());
        taskTemplateVersionRepository.insert(entity);
        log.info("新增任务模板版本成功, taskTemplateCode={}, version={}, id={}",
                entity.getTaskTemplateCode(), entity.getVersion(), entity.getId());
        return toResponse(taskTemplateVersionRepository.selectById(entity.getId()));
    }

    @Override
    public TaskTemplateVersionResponse update(TaskTemplateVersionUpdateRequest request) {
        requireCodeAndVersion(request.getTaskTemplateCode(), request.getVersion());
        TaskTemplateVersion existing = taskTemplateVersionRepository.selectByCodeAndVersion(
                request.getTaskTemplateCode(), request.getVersion());
        if (existing == null) {
            throw new BusinessException(TaskTemplateConstants.MSG_VERSION_NOT_EXIST);
        }
        // 仅草稿状态的版本允许修改（生效中 / 已失效需通过「新增版本」走版本变更）
        if (!TaskTemplateVersionStatusEnum.DRAFT.getCode().equals(existing.getStatus())) {
            throw new BusinessException(String.format(
                    TaskTemplateConstants.MSG_VERSION_UPDATE_STATUS_INVALID, existing.getStatus()));
        }
        // 模板详情必填，且需能解析为合法的 Argo WorkflowTemplate
        if (!StringUtils.hasText(request.getTemplateDetail())) {
            throw new BusinessException(TaskTemplateConstants.MSG_TEMPLATE_DETAIL_REQUIRED);
        }
        IoArgoprojWorkflowV1alpha1WorkflowTemplate workflowTemplate = parseWorkflowTemplate(request.getTemplateDetail());
        // 校验模板详情 metadata.name 与任务模板编码一致
        validateTemplateNameMatchCode(workflowTemplate, request.getTaskTemplateCode());
        // 仅允许修改 templateDetail / changeNote，version 为业务键不可改、status 走状态变更接口
        TaskTemplateVersion entity = new TaskTemplateVersion();
        entity.setId(existing.getId());
        entity.setTemplateDetail(request.getTemplateDetail());
        entity.setChangeNote(request.getChangeNote());
        taskTemplateVersionRepository.updateById(entity);
        log.info("修改任务模板版本成功, taskTemplateCode={}, version={}",
                request.getTaskTemplateCode(), request.getVersion());
        return toResponse(taskTemplateVersionRepository.selectByCodeAndVersion(
                request.getTaskTemplateCode(), request.getVersion()));
    }

    @Override
    public void deleteById(Long id) {
        TaskTemplateVersion existing = taskTemplateVersionRepository.selectById(id);
        if (existing == null) {
            throw new BusinessException(TaskTemplateConstants.MSG_VERSION_NOT_EXIST);
        }
        // 落库前先打通 argo：按名称删除对应的 WorkflowTemplate
        IoArgoprojWorkflowV1alpha1WorkflowTemplate workflowTemplate = parseWorkflowTemplate(existing.getTemplateDetail());
        String templateName = getWorkflowTemplateName(workflowTemplate);
        argoWorkflowAgent.deleteWorkflowTemplate(argoServerProperties.getNamespace(), templateName);
        taskTemplateVersionRepository.deleteById(id);
        log.info("删除任务模板版本成功, id={}, taskTemplateCode={}, version={}",
                id, existing.getTaskTemplateCode(), existing.getVersion());
    }

    @Override
    public TaskTemplateVersionResponse getDetail(String taskTemplateCode, String version) {
        requireCodeAndVersion(taskTemplateCode, version);
        TaskTemplateVersion entity = taskTemplateVersionRepository.selectByCodeAndVersion(taskTemplateCode, version);
        if (entity == null) {
            throw new BusinessException(TaskTemplateConstants.MSG_VERSION_NOT_EXIST);
        }
        return toResponse(entity);
    }

    @Override
    public List<TaskTemplateVersionResponse> listByCode(String taskTemplateCode) {
        if (!StringUtils.hasText(taskTemplateCode)) {
            throw new BusinessException(TaskTemplateConstants.MSG_TEMPLATE_CODE_REQUIRED);
        }
        List<TaskTemplateVersion> versions = taskTemplateVersionRepository.listByCode(taskTemplateCode);
        // 按创建时间倒序（代码内排序，非 SQL），列表项不返回模板详情（减少传输）
        return versions.stream()
                .sorted(Comparator.comparing(TaskTemplateVersion::getCreateTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toListResponse)
                .collect(Collectors.toList());
    }

    @Override
    public TaskTemplateVersionResponse changeStatus(TaskTemplateVersionStatusRequest request) {
        if (request == null || !StringUtils.hasText(request.getStatus())) {
            throw new BusinessException(String.format(
                    TaskTemplateConstants.MSG_VERSION_STATUS_INVALID, request == null ? null : request.getStatus()));
        }
        requireCodeAndVersion(request.getTaskTemplateCode(), request.getVersion());
        // 目标状态必须是合法枚举
        if (!TaskTemplateVersionStatusEnum.isValidCode(request.getStatus())) {
            throw new BusinessException(String.format(
                    TaskTemplateConstants.MSG_VERSION_STATUS_INVALID, request.getStatus()));
        }
        TaskTemplateVersion existing = taskTemplateVersionRepository.selectByCodeAndVersion(
                request.getTaskTemplateCode(), request.getVersion());
        if (existing == null) {
            throw new BusinessException(TaskTemplateConstants.MSG_VERSION_NOT_EXIST);
        }

        // 幂等：目标状态与当前状态一致时直接返回，不重复触发副作用（如失效其它版本）
        if (existing.getStatus().equals(request.getStatus())) {
            log.info("任务模板版本状态未变化，跳过更新（幂等）, taskTemplateCode={}, version={}, status={}",
                    request.getTaskTemplateCode(), request.getVersion(), request.getStatus());
            return toResponse(existing);
        }

        // 发布（目标为生效中）：落库前先打通 argo——不存在则创建，存在则更新；
        // 并校验模板详情 metadata.name 与任务模板编码一致
        if (TaskTemplateVersionStatusEnum.EFFECTIVE.getCode().equals(request.getStatus())) {
            IoArgoprojWorkflowV1alpha1WorkflowTemplate workflowTemplate =
                    parseWorkflowTemplate(existing.getTemplateDetail());
            validateTemplateNameMatchCode(workflowTemplate, request.getTaskTemplateCode());
            argoWorkflowAgent.saveWorkflowTemplate(argoServerProperties.getNamespace(), workflowTemplate);
        }

        // 目标为生效中时，其它尚未失效的版本（生效中 / 草稿）统一自动置为已失效
        if (TaskTemplateVersionStatusEnum.EFFECTIVE.getCode().equals(request.getStatus())) {
            taskTemplateVersionRepository.updateOtherStatusToExpired(
                    request.getTaskTemplateCode(), request.getVersion());
        }
        taskTemplateVersionRepository.updateStatusByCodeAndVersion(
                request.getTaskTemplateCode(), request.getVersion(), request.getStatus());
        log.info("变更任务模板版本状态成功, taskTemplateCode={}, version={}, status={}",
                request.getTaskTemplateCode(), request.getVersion(), request.getStatus());
        return toResponse(taskTemplateVersionRepository.selectByCodeAndVersion(
                request.getTaskTemplateCode(), request.getVersion()));
    }

    // ===== 私有工具方法 =====

    /**
     * 校验 task_template_code 与 version 非空
     */
    private void requireCodeAndVersion(String taskTemplateCode, String version) {
        if (!StringUtils.hasText(taskTemplateCode)) {
            throw new BusinessException(TaskTemplateConstants.MSG_TEMPLATE_CODE_REQUIRED);
        }
        if (!StringUtils.hasText(version)) {
            throw new BusinessException(TaskTemplateConstants.MSG_VERSION_REQUIRED);
        }
    }

    /**
     * 解析版本号为三段整数，格式非法抛业务异常
     */
    private int[] parseVersion(String version) {
        if (!VERSION_PATTERN.matcher(version).matches()) {
            throw new BusinessException(TaskTemplateConstants.MSG_VERSION_FORMAT_INVALID);
        }
        String[] parts = version.split("\\.");
        int[] tuple = new int[3];
        for (int i = 0; i < 3; i++) {
            tuple[i] = Integer.parseInt(parts[i]);
        }
        return tuple;
    }

    /**
     * 版本递增规则校验（相对当前最大版本）：
     * <ul>
     *     <li>无历史版本时（首个版本）：格式合法即可；</li>
     *     <li>有历史版本时：整体必须严格更大，且每个段相对最大版本对应段增量 ≤ +1（允许某段归零/变小）。</li>
     * </ul>
     *
     * @param taskTemplateCode 任务模板编码
     * @param newVer           传入版本的三段整数
     * @param rawVersion       传入版本原始字符串（用于错误提示）
     */
    private void validateIncrement(String taskTemplateCode, int[] newVer, String rawVersion) {
        List<String> existing = taskTemplateVersionRepository.listVersionsByCode(taskTemplateCode);
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
                    TaskTemplateConstants.MSG_VERSION_INCREMENT_INVALID, maxRaw, rawVersion));
        }
        // 每段相对最大版本对应段增量 ≤ +1（允许变小/归零）
        for (int i = 0; i < 3; i++) {
            if (newVer[i] - max[i] > 1) {
                throw new BusinessException(String.format(
                        TaskTemplateConstants.MSG_VERSION_INCREMENT_INVALID, maxRaw, rawVersion));
            }
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
                    TaskTemplateConstants.MSG_TEMPLATE_DETAIL_INVALID, e.getMessage()));
        }
    }

    /**
     * 校验 WorkflowTemplate 的 metadata.name 与任务模板编码一致，不一致抛业务异常。
     * <p>argo WorkflowTemplate 以任务模板编码命名，所有版本共享同一个 argo 模板，
     * 故模板详情中的 name 必须与所属任务模板编码严格相等。
     */
    private void validateTemplateNameMatchCode(IoArgoprojWorkflowV1alpha1WorkflowTemplate workflowTemplate,
                                               String taskTemplateCode) {
        String name = getWorkflowTemplateName(workflowTemplate);
        if (!taskTemplateCode.equals(name)) {
            throw new BusinessException(String.format(
                    TaskTemplateConstants.MSG_TEMPLATE_NAME_NOT_MATCH_CODE, name, taskTemplateCode));
        }
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
            throw new BusinessException(TaskTemplateConstants.MSG_TEMPLATE_NAME_REQUIRED);
        }
        return name;
    }

    private TaskTemplateVersionResponse toResponse(TaskTemplateVersion entity) {
        if (entity == null) {
            return null;
        }
        TaskTemplateVersionResponse response = new TaskTemplateVersionResponse();
        BeanUtils.copyProperties(entity, response);
        return response;
    }

    /**
     * 列表用响应：不携带 templateDetail（配合全局 non_null 序列化省略）
     */
    private TaskTemplateVersionResponse toListResponse(TaskTemplateVersion entity) {
        TaskTemplateVersionResponse response = toResponse(entity);
        response.setTemplateDetail(null);
        return response;
    }
}
