package com.ci.pipeline.service.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ci.pipeline.common.auth.UserContext;
import com.ci.pipeline.common.constants.CommonConstants;
import com.ci.pipeline.common.constants.DistributedLockConstants;
import com.ci.pipeline.common.constants.TaskTemplateConstants;
import com.ci.pipeline.common.exception.BusinessException;
import com.ci.pipeline.common.util.SortUtil;
import com.ci.pipeline.dao.entity.TaskTemplate;
import com.ci.pipeline.dao.repository.PipelineTemplateRepository;
import com.ci.pipeline.dao.repository.TaskTemplateRepository;
import com.ci.pipeline.dao.repository.TaskTemplateVersionRepository;
import com.ci.pipeline.facade.request.TaskTemplateCreateRequest;
import com.ci.pipeline.facade.request.TaskTemplateQueryRequest;
import com.ci.pipeline.facade.request.TaskTemplateUpdateRequest;
import com.ci.pipeline.facade.response.DictDataResponse;
import com.ci.pipeline.facade.response.PageResponse;
import com.ci.pipeline.facade.response.TaskTemplateResponse;
import com.ci.pipeline.service.service.DictDataService;
import com.ci.pipeline.service.service.DistributedLockService;
import com.ci.pipeline.service.service.TaskTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 任务模板业务实现
 */
@Slf4j
@Service
public class TaskTemplateServiceImpl implements TaskTemplateService {

    /**
     * 分页排序字段白名单：出参字段名（camelCase） → 数据库列名（snake_case）
     */
    private static final Map<String, String> SORT_FIELD_MAP;
    static {
        Map<String, String> m = new HashMap<>();
        m.put("id", "id");
        m.put("taskTemplateCode", "task_template_code");
        m.put("name", "name");
        m.put("description", "description");
        m.put("taskTemplateGroup", "task_template_group");
        m.put("creator", "creator");
        m.put("createTime", "create_time");
        m.put("updateTime", "update_time");
        SORT_FIELD_MAP = Collections.unmodifiableMap(m);
    }

    /** 任务模板编码格式：小写字母，多段用 - 连接 */
    private static final Pattern CODE_PATTERN = Pattern.compile(TaskTemplateConstants.CODE_REGEX);

    @Autowired
    private TaskTemplateRepository taskTemplateRepository;

    @Autowired
    private TaskTemplateVersionRepository taskTemplateVersionRepository;

    @Autowired
    private PipelineTemplateRepository pipelineTemplateRepository;

    @Autowired
    private DictDataService dictDataService;

    @Autowired
    private DistributedLockService distributedLockService;

    @Override
    public TaskTemplateResponse create(TaskTemplateCreateRequest request) {
        validateRequired(request);
        validateCodeFormat(request.getTaskTemplateCode());
        // 非阻塞加锁，防止并发或连击
        String lockKey = DistributedLockConstants.LOCK_KEY_TASK_TEMPLATE + request.getTaskTemplateCode();
        String lockValue = distributedLockService.tryLock(
                lockKey, DistributedLockConstants.DEFAULT_LOCK_EXPIRE_SECONDS, "新增任务模板");
        if (lockValue == null) {
            throw new BusinessException(TaskTemplateConstants.MSG_OPERATION_LOCK_FAILED);
        }
        try {
            // 唯一性校验：task_template_code 在未删除记录中唯一
            if (taskTemplateRepository.countByTaskTemplateCode(request.getTaskTemplateCode(), null) > 0) {
                throw new BusinessException(String.format(
                        TaskTemplateConstants.MSG_TEMPLATE_CODE_DUPLICATED, request.getTaskTemplateCode()));
            }
            // 跨表唯一性校验：任务模板与流水线模板在 argo 侧共享命名空间，编码需全局唯一
            if (pipelineTemplateRepository.countByPipelineTemplateCode(request.getTaskTemplateCode(), null) > 0) {
                throw new BusinessException(String.format(
                        TaskTemplateConstants.MSG_TEMPLATE_CODE_CONFLICT_PIPELINE, request.getTaskTemplateCode()));
            }
            TaskTemplate entity = new TaskTemplate();
            BeanUtils.copyProperties(request, entity);
            // 创建人取当前登录用户（Controller 已 @RequireLogin，保证非空）
            entity.setCreator(UserContext.getUserId());
            taskTemplateRepository.insert(entity);
            log.info("新增任务模板成功, taskTemplateCode={}, id={}", entity.getTaskTemplateCode(), entity.getId());
            return toResponse(taskTemplateRepository.selectById(entity.getId()));
        } finally {
            distributedLockService.unlock(lockKey, lockValue);
        }
    }

    @Override
    public TaskTemplateResponse update(TaskTemplateUpdateRequest request) {
        if (request.getId() == null) {
            throw new BusinessException(TaskTemplateConstants.MSG_TEMPLATE_ID_REQUIRED);
        }
        TaskTemplate existing = taskTemplateRepository.selectById(request.getId());
        if (existing == null) {
            throw new BusinessException(TaskTemplateConstants.MSG_TEMPLATE_NOT_EXIST);
        }
        // 若传入 task_template_code，需校验非空
        if (request.getTaskTemplateCode() != null && !StringUtils.hasText(request.getTaskTemplateCode())) {
            throw new BusinessException(TaskTemplateConstants.MSG_TEMPLATE_CODE_REQUIRED);
        }
        // 若传入新编码，校验格式
        if (request.getTaskTemplateCode() != null) {
            validateCodeFormat(request.getTaskTemplateCode());
        }
        // 非阻塞加锁，防止并发或连击（以实际生效的编码为锁粒度）
        String effectiveCode = request.getTaskTemplateCode() != null
                ? request.getTaskTemplateCode() : existing.getTaskTemplateCode();
        String lockKey = DistributedLockConstants.LOCK_KEY_TASK_TEMPLATE + effectiveCode;
        String lockValue = distributedLockService.tryLock(
                lockKey, DistributedLockConstants.DEFAULT_LOCK_EXPIRE_SECONDS, "修改任务模板");
        if (lockValue == null) {
            throw new BusinessException(TaskTemplateConstants.MSG_OPERATION_LOCK_FAILED);
        }
        try {
            // 唯一性校验：排除自身
            if (request.getTaskTemplateCode() != null
                    && taskTemplateRepository.countByTaskTemplateCode(
                            request.getTaskTemplateCode(), request.getId()) > 0) {
                throw new BusinessException(String.format(
                        TaskTemplateConstants.MSG_TEMPLATE_CODE_DUPLICATED, request.getTaskTemplateCode()));
            }
            // 跨表唯一性校验（编码变更时）
            if (request.getTaskTemplateCode() != null
                    && pipelineTemplateRepository.countByPipelineTemplateCode(
                            request.getTaskTemplateCode(), null) > 0) {
                throw new BusinessException(String.format(
                        TaskTemplateConstants.MSG_TEMPLATE_CODE_CONFLICT_PIPELINE, request.getTaskTemplateCode()));
            }
            TaskTemplate entity = new TaskTemplate();
            BeanUtils.copyProperties(request, entity);
            // creator 由系统维护，更新时不允许修改
            entity.setCreator(null);
            taskTemplateRepository.updateById(entity);
            log.info("修改任务模板成功, id={}", request.getId());
            return toResponse(taskTemplateRepository.selectById(request.getId()));
        } finally {
            distributedLockService.unlock(lockKey, lockValue);
        }
    }

    @Override
    public void deleteById(Long id) {
        TaskTemplate existing = taskTemplateRepository.selectById(id);
        if (existing == null) {
            throw new BusinessException(TaskTemplateConstants.MSG_TEMPLATE_NOT_EXIST);
        }
        // 删除前校验：该模板下若存在版本，则禁止删除
        long versionCount = taskTemplateVersionRepository.countByCode(existing.getTaskTemplateCode());
        if (versionCount > 0) {
            throw new BusinessException(String.format(
                    TaskTemplateConstants.MSG_TEMPLATE_HAS_VERSION, existing.getTaskTemplateCode()));
        }
        taskTemplateRepository.deleteById(id);
        log.info("删除任务模板成功, id={}, taskTemplateCode={}", id, existing.getTaskTemplateCode());
    }

    @Override
    public TaskTemplateResponse getById(Long id) {
        TaskTemplate entity = taskTemplateRepository.selectById(id);
        if (entity == null) {
            throw new BusinessException(TaskTemplateConstants.MSG_TEMPLATE_NOT_EXIST);
        }
        return toResponse(entity);
    }

    @Override
    public PageResponse<TaskTemplateResponse> page(TaskTemplateQueryRequest query) {
        long pageNum = query.getPageNum() == null ? CommonConstants.DEFAULT_PAGE_NUM : query.getPageNum();
        long pageSize = query.getPageSize() == null ? CommonConstants.DEFAULT_PAGE_SIZE : query.getPageSize();
        // 解析排序字段（白名单映射）与方向（默认 desc）；sortField 为空时走默认排序
        String sortField = SortUtil.resolveField(query.getSortField(), SORT_FIELD_MAP);
        String sortOrder = sortField != null ? SortUtil.resolveOrder(query.getSortOrder()) : null;
        IPage<TaskTemplate> pageResult = taskTemplateRepository.pageQuery(
                pageNum, pageSize, query.getTaskTemplateCode(), query.getName(),
                query.getTaskTemplateGroup(), sortField, sortOrder);
        List<TaskTemplateResponse> records = pageResult.getRecords().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return PageResponse.of(records, pageResult.getTotal(), pageResult.getCurrent(), pageResult.getSize(), pageResult.getPages());
    }

    @Override
    public List<DictDataResponse> listGroups() {
        return dictDataService.listByDictType(TaskTemplateConstants.DICT_TYPE_TASK_TEMPLATE_GROUP);
    }

    /**
     * 校验任务模板编码格式：小写字母，多段用 - 连接
     */
    private void validateCodeFormat(String code) {
        if (!CODE_PATTERN.matcher(code).matches()) {
            throw new BusinessException(String.format(
                    TaskTemplateConstants.MSG_TEMPLATE_CODE_FORMAT_INVALID, code));
        }
    }

    /**
     * 新增必填字段校验
     */
    private void validateRequired(TaskTemplateCreateRequest request) {
        if (!StringUtils.hasText(request.getTaskTemplateCode())) {
            throw new BusinessException(TaskTemplateConstants.MSG_TEMPLATE_CODE_REQUIRED);
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new BusinessException(TaskTemplateConstants.MSG_TEMPLATE_NAME_REQUIRED);
        }
        if (!StringUtils.hasText(request.getTaskTemplateGroup())) {
            throw new BusinessException(TaskTemplateConstants.MSG_TEMPLATE_GROUP_REQUIRED);
        }
    }

    private TaskTemplateResponse toResponse(TaskTemplate entity) {
        if (entity == null) {
            return null;
        }
        TaskTemplateResponse response = new TaskTemplateResponse();
        BeanUtils.copyProperties(entity, response);
        return response;
    }
}
