package com.ci.pipeline.service.service.impl;

import com.ci.pipeline.common.auth.UserContext;
import com.ci.pipeline.common.constants.PipelineTemplateConstants;
import com.ci.pipeline.common.exception.BusinessException;
import com.ci.pipeline.common.util.SortUtil;
import com.ci.pipeline.dao.entity.PipelineTemplate;
import com.ci.pipeline.dao.repository.PipelineTemplateRepository;
import com.ci.pipeline.dao.repository.PipelineTemplateVersionRepository;
import com.ci.pipeline.facade.request.PipelineTemplateCreateRequest;
import com.ci.pipeline.facade.request.PipelineTemplateQueryRequest;
import com.ci.pipeline.facade.request.PipelineTemplateUpdateRequest;
import com.ci.pipeline.facade.response.DictDataResponse;
import com.ci.pipeline.facade.response.PipelineTemplateResponse;
import com.ci.pipeline.service.service.DictDataService;
import com.ci.pipeline.service.service.PipelineTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 流水线模板业务实现
 */
@Slf4j
@Service
public class PipelineTemplateServiceImpl implements PipelineTemplateService {

    /**
     * 列表排序字段白名单：出参字段名（camelCase） → 数据库列名（snake_case）
     */
    private static final Map<String, String> SORT_FIELD_MAP;
    static {
        Map<String, String> m = new HashMap<>();
        m.put("id", "id");
        m.put("pipelineTemplateCode", "pipeline_template_code");
        m.put("name", "name");
        m.put("description", "description");
        m.put("pipelineTemplateGroup", "pipeline_template_group");
        m.put("creator", "creator");
        m.put("createTime", "create_time");
        m.put("updateTime", "update_time");
        SORT_FIELD_MAP = Collections.unmodifiableMap(m);
    }

    @Autowired
    private PipelineTemplateRepository pipelineTemplateRepository;

    @Autowired
    private PipelineTemplateVersionRepository pipelineTemplateVersionRepository;

    @Autowired
    private DictDataService dictDataService;

    @Override
    public PipelineTemplateResponse create(PipelineTemplateCreateRequest request) {
        validateRequired(request);
        // 唯一性校验：pipeline_template_code 在未删除记录中唯一
        if (pipelineTemplateRepository.countByPipelineTemplateCode(request.getPipelineTemplateCode(), null) > 0) {
            throw new BusinessException(String.format(
                    PipelineTemplateConstants.MSG_TEMPLATE_CODE_DUPLICATED, request.getPipelineTemplateCode()));
        }
        PipelineTemplate entity = new PipelineTemplate();
        BeanUtils.copyProperties(request, entity);
        // 创建人取当前登录用户（Controller 已 @RequireLogin，保证非空）
        entity.setCreator(UserContext.getUserId());
        pipelineTemplateRepository.insert(entity);
        log.info("新增流水线模板成功, pipelineTemplateCode={}, id={}",
                entity.getPipelineTemplateCode(), entity.getId());
        return toResponse(pipelineTemplateRepository.selectById(entity.getId()));
    }

    @Override
    public PipelineTemplateResponse update(PipelineTemplateUpdateRequest request) {
        if (request.getId() == null) {
            throw new BusinessException(PipelineTemplateConstants.MSG_TEMPLATE_ID_REQUIRED);
        }
        PipelineTemplate existing = pipelineTemplateRepository.selectById(request.getId());
        if (existing == null) {
            throw new BusinessException(PipelineTemplateConstants.MSG_TEMPLATE_NOT_EXIST);
        }
        // 若传入 pipeline_template_code，需校验非空
        if (request.getPipelineTemplateCode() != null && !StringUtils.hasText(request.getPipelineTemplateCode())) {
            throw new BusinessException(PipelineTemplateConstants.MSG_TEMPLATE_CODE_REQUIRED);
        }
        // 唯一性校验：排除自身
        if (request.getPipelineTemplateCode() != null
                && pipelineTemplateRepository.countByPipelineTemplateCode(
                        request.getPipelineTemplateCode(), request.getId()) > 0) {
            throw new BusinessException(String.format(
                    PipelineTemplateConstants.MSG_TEMPLATE_CODE_DUPLICATED, request.getPipelineTemplateCode()));
        }
        PipelineTemplate entity = new PipelineTemplate();
        BeanUtils.copyProperties(request, entity);
        // creator 由系统维护，更新时不允许修改
        entity.setCreator(null);
        pipelineTemplateRepository.updateById(entity);
        log.info("修改流水线模板成功, id={}", request.getId());
        return toResponse(pipelineTemplateRepository.selectById(request.getId()));
    }

    @Override
    public void deleteById(Long id) {
        PipelineTemplate existing = pipelineTemplateRepository.selectById(id);
        if (existing == null) {
            throw new BusinessException(PipelineTemplateConstants.MSG_TEMPLATE_NOT_EXIST);
        }
        // 删除前校验：该模板下若存在版本，则禁止删除
        long versionCount = pipelineTemplateVersionRepository.countByCode(existing.getPipelineTemplateCode());
        if (versionCount > 0) {
            throw new BusinessException(String.format(
                    PipelineTemplateConstants.MSG_TEMPLATE_HAS_VERSION, existing.getPipelineTemplateCode()));
        }
        pipelineTemplateRepository.deleteById(id);
        log.info("删除流水线模板成功, id={}, pipelineTemplateCode={}", id, existing.getPipelineTemplateCode());
    }

    @Override
    public PipelineTemplateResponse getById(Long id) {
        PipelineTemplate entity = pipelineTemplateRepository.selectById(id);
        if (entity == null) {
            throw new BusinessException(PipelineTemplateConstants.MSG_TEMPLATE_NOT_EXIST);
        }
        return toResponse(entity);
    }

    @Override
    public List<PipelineTemplateResponse> list(PipelineTemplateQueryRequest query) {
        // 解析排序字段（白名单映射）与方向（默认 desc）；sortField 为空时走默认排序
        String sortField = SortUtil.resolveField(query.getSortField(), SORT_FIELD_MAP);
        String sortOrder = sortField != null ? SortUtil.resolveOrder(query.getSortOrder()) : null;
        List<PipelineTemplate> entities = pipelineTemplateRepository.listQuery(
                query.getPipelineTemplateGroup(), sortField, sortOrder);
        return entities.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<DictDataResponse> listGroups() {
        return dictDataService.listByDictType(PipelineTemplateConstants.DICT_TYPE_PIPELINE_TEMPLATE_GROUP);
    }

    /**
     * 新增必填字段校验
     */
    private void validateRequired(PipelineTemplateCreateRequest request) {
        if (!StringUtils.hasText(request.getPipelineTemplateCode())) {
            throw new BusinessException(PipelineTemplateConstants.MSG_TEMPLATE_CODE_REQUIRED);
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new BusinessException(PipelineTemplateConstants.MSG_TEMPLATE_NAME_REQUIRED);
        }
        if (!StringUtils.hasText(request.getPipelineTemplateGroup())) {
            throw new BusinessException(PipelineTemplateConstants.MSG_TEMPLATE_GROUP_REQUIRED);
        }
    }

    private PipelineTemplateResponse toResponse(PipelineTemplate entity) {
        if (entity == null) {
            return null;
        }
        PipelineTemplateResponse response = new PipelineTemplateResponse();
        BeanUtils.copyProperties(entity, response);
        return response;
    }
}
