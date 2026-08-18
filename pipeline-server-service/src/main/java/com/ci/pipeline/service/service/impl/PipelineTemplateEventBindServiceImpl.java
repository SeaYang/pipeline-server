package com.ci.pipeline.service.service.impl;

import com.ci.pipeline.common.auth.UserContext;
import com.ci.pipeline.common.constants.CommonConstants;
import com.ci.pipeline.common.constants.PipelineConstants;
import com.ci.pipeline.common.constants.PipelineEventConstants;
import com.ci.pipeline.common.exception.BusinessException;
import com.ci.pipeline.common.util.SortUtil;
import com.ci.pipeline.dao.entity.DictData;
import com.ci.pipeline.dao.entity.PipelineTemplate;
import com.ci.pipeline.dao.entity.PipelineTemplateEventBind;
import com.ci.pipeline.dao.repository.DictDataRepository;
import com.ci.pipeline.dao.repository.PipelineTemplateEventBindRepository;
import com.ci.pipeline.dao.repository.PipelineTemplateRepository;
import com.ci.pipeline.facade.request.PipelineTemplateEventBindCreateRequest;
import com.ci.pipeline.facade.request.PipelineTemplateEventBindQueryRequest;
import com.ci.pipeline.facade.request.PipelineTemplateEventBindUpdateRequest;
import com.ci.pipeline.facade.response.PageResponse;
import com.ci.pipeline.facade.response.PipelineTemplateEventBindResponse;
import com.ci.pipeline.service.service.PipelineTemplateEventBindService;
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
 * 事件-模板绑定管理服务实现（后台配置）
 */
@Slf4j
@Service
public class PipelineTemplateEventBindServiceImpl implements PipelineTemplateEventBindService {

    /**
     * 排序字段白名单：camelCase → snake_case
     */
    private static final Map<String, String> SORT_FIELD_MAP;
    static {
        Map<String, String> m = new HashMap<>();
        m.put("id", "id");
        m.put("eventType", "event_type");
        m.put("pipelineTemplateCode", "pipeline_template_code");
        m.put("creator", "creator");
        m.put("createTime", "create_time");
        m.put("updateTime", "update_time");
        SORT_FIELD_MAP = Collections.unmodifiableMap(m);
    }

    @Autowired
    private PipelineTemplateEventBindRepository repository;

    @Autowired
    private DictDataRepository dictDataRepository;

    @Autowired
    private PipelineTemplateRepository pipelineTemplateRepository;

    @Override
    public PipelineTemplateEventBindResponse create(PipelineTemplateEventBindCreateRequest request) {
        validateRequired(request.getEventType(), request.getPipelineTemplateCode());

        // 校验事件类型在字典中存在且启用
        validateEventTypeInDict(request.getEventType());

        // 校验模板编码存在
        validateTemplateExists(request.getPipelineTemplateCode());

        // 唯一性校验
        long count = repository.countByEventTypeAndTemplateCode(
                request.getEventType(), request.getPipelineTemplateCode(), null);
        if (count > 0) {
            throw new BusinessException(String.format(
                    PipelineEventConstants.MSG_EVENT_BIND_DUPLICATED,
                    request.getEventType(), request.getPipelineTemplateCode()));
        }

        PipelineTemplateEventBind entity = new PipelineTemplateEventBind();
        BeanUtils.copyProperties(request, entity);
        entity.setCreator(UserContext.getUserId());
        repository.insert(entity);
        log.info("新增事件-模板绑定成功, eventType={}, templateCode={}, id={}",
                entity.getEventType(), entity.getPipelineTemplateCode(), entity.getId());
        return toResponse(repository.selectById(entity.getId()));
    }

    @Override
    public PipelineTemplateEventBindResponse update(PipelineTemplateEventBindUpdateRequest request) {
        if (request.getId() == null) {
            throw new BusinessException("id不能为空");
        }
        PipelineTemplateEventBind existing = repository.selectById(request.getId());
        if (existing == null) {
            throw new BusinessException(PipelineEventConstants.MSG_EVENT_BIND_NOT_EXIST);
        }

        validateRequired(request.getEventType(), request.getPipelineTemplateCode());
        validateEventTypeInDict(request.getEventType());
        validateTemplateExists(request.getPipelineTemplateCode());

        // 唯一性校验（排除自身）
        long count = repository.countByEventTypeAndTemplateCode(
                request.getEventType(), request.getPipelineTemplateCode(), request.getId());
        if (count > 0) {
            throw new BusinessException(String.format(
                    PipelineEventConstants.MSG_EVENT_BIND_DUPLICATED,
                    request.getEventType(), request.getPipelineTemplateCode()));
        }

        BeanUtils.copyProperties(request, existing);
        repository.updateById(existing);
        log.info("修改事件-模板绑定成功, id={}", existing.getId());
        return toResponse(repository.selectById(existing.getId()));
    }

    @Override
    public void deleteById(Long id) {
        PipelineTemplateEventBind existing = repository.selectById(id);
        if (existing == null) {
            throw new BusinessException(PipelineEventConstants.MSG_EVENT_BIND_NOT_EXIST);
        }
        repository.deleteById(id);
        log.info("删除事件-模板绑定成功, id={}, eventType={}, templateCode={}",
                id, existing.getEventType(), existing.getPipelineTemplateCode());
    }

    @Override
    public PipelineTemplateEventBindResponse getById(Long id) {
        PipelineTemplateEventBind entity = repository.selectById(id);
        if (entity == null) {
            throw new BusinessException(PipelineEventConstants.MSG_EVENT_BIND_NOT_EXIST);
        }
        return toResponse(entity);
    }

    @Override
    public PageResponse<PipelineTemplateEventBindResponse> page(PipelineTemplateEventBindQueryRequest query) {
        long pageNum = query.getPageNum() == null ? CommonConstants.DEFAULT_PAGE_NUM : query.getPageNum();
        long pageSize = query.getPageSize() == null ? CommonConstants.DEFAULT_PAGE_SIZE : query.getPageSize();
        String sortField = SortUtil.resolveField(query.getSortField(), SORT_FIELD_MAP);
        String sortOrder = sortField != null ? SortUtil.resolveOrder(query.getSortOrder()) : null;

        com.baomidou.mybatisplus.core.metadata.IPage<PipelineTemplateEventBind> pageResult =
                repository.pageQuery(pageNum, pageSize, query.getEventType(), sortField, sortOrder);
        List<PipelineTemplateEventBind> records = pageResult.getRecords();
        if (records == null) {
            records = Collections.emptyList();
        }
        List<PipelineTemplateEventBindResponse> responseList = records.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return PageResponse.of(responseList, pageResult.getTotal(), pageResult.getCurrent(),
                pageResult.getSize(), pageResult.getPages());
    }

    @Override
    public List<String> listTemplateCodesByEventType(String eventType) {
        return repository.listTemplateCodesByEventType(eventType);
    }

    // ===== 私有方法 =====

    private void validateRequired(String eventType, String pipelineTemplateCode) {
        if (!StringUtils.hasText(eventType)) {
            throw new BusinessException(PipelineEventConstants.MSG_EVENT_TYPE_REQUIRED);
        }
        if (!StringUtils.hasText(pipelineTemplateCode)) {
            throw new BusinessException(PipelineEventConstants.MSG_TEMPLATE_CODE_REQUIRED);
        }
    }

    /**
     * 校验事件类型在字典 pipeline-event-type 中存在且启用
     */
    private void validateEventTypeInDict(String eventType) {
        DictData dictData = dictDataRepository.selectByTypeAndKey(
                PipelineEventConstants.DICT_TYPE_PIPELINE_EVENT_TYPE, eventType);
        if (dictData == null || !Boolean.TRUE.equals(dictData.getEnabled())) {
            throw new BusinessException(String.format(
                    PipelineEventConstants.MSG_EVENT_TYPE_NOT_IN_DICT, eventType));
        }
    }

    /**
     * 校验流水线模板编码存在
     */
    private void validateTemplateExists(String pipelineTemplateCode) {
        PipelineTemplate template = pipelineTemplateRepository.selectByPipelineTemplateCode(pipelineTemplateCode);
        if (template == null) {
            throw new BusinessException(String.format(
                    PipelineConstants.MSG_TEMPLATE_NOT_EXIST, pipelineTemplateCode));
        }
    }

    /**
     * Entity → Response，关联翻译事件类型中文名和模板名称
     */
    private PipelineTemplateEventBindResponse toResponse(PipelineTemplateEventBind entity) {
        if (entity == null) {
            return null;
        }
        PipelineTemplateEventBindResponse response = new PipelineTemplateEventBindResponse();
        BeanUtils.copyProperties(entity, response);

        // 翻译事件类型中文名
        DictData dictData = dictDataRepository.selectByTypeAndKey(
                PipelineEventConstants.DICT_TYPE_PIPELINE_EVENT_TYPE, entity.getEventType());
        if (dictData != null) {
            response.setEventTypeDesc(dictData.getDictValue());
        }

        // 翻译模板名称
        PipelineTemplate template = pipelineTemplateRepository.selectByPipelineTemplateCode(
                entity.getPipelineTemplateCode());
        if (template != null) {
            response.setPipelineTemplateName(template.getName());
        }

        return response;
    }
}
