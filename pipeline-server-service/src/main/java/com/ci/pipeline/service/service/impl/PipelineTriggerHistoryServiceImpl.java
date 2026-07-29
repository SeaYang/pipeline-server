package com.ci.pipeline.service.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ci.pipeline.common.constants.CommonConstants;
import com.ci.pipeline.common.constants.PipelineTriggerHistoryConstants;
import com.ci.pipeline.common.exception.BusinessException;
import com.ci.pipeline.dao.entity.PipelineTriggerHistory;
import com.ci.pipeline.dao.repository.PipelineTriggerHistoryRepository;
import com.ci.pipeline.facade.request.PipelineTriggerHistoryQueryRequest;
import com.ci.pipeline.facade.response.PageResponse;
import com.ci.pipeline.facade.response.PipelineTriggerHistoryResponse;
import com.ci.pipeline.service.service.PipelineTriggerHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PipelineTriggerHistoryServiceImpl implements PipelineTriggerHistoryService {

    @Autowired
    private PipelineTriggerHistoryRepository pipelineTriggerHistoryRepository;

    @Override
    public void add(String appName, Long pipelineId, Long pipelineRunId, Long pipelineEventBindId,
                    String status, String type, String creator, String requestBody,
                    String errorMessage, String pipelineTemplateCode, String pipelineTemplateVersion) {
        try {
            PipelineTriggerHistory entity = new PipelineTriggerHistory();
            entity.setAppName(appName);
            entity.setPipelineId(pipelineId);
            entity.setPipelineRunId(pipelineRunId);
            entity.setPipelineEventBindId(pipelineEventBindId);
            entity.setStatus(status);
            entity.setType(type);
            entity.setCreator(creator);
            entity.setRequestBody(requestBody);
            entity.setErrorMessage(errorMessage);
            entity.setPipelineTemplateCode(pipelineTemplateCode);
            entity.setPipelineTemplateVersion(pipelineTemplateVersion);
            pipelineTriggerHistoryRepository.insert(entity);
        } catch (Exception e) {
            // 触发历史是辅助功能，记录失败只打日志，不影响主流程
            log.error("记录触发历史失败, appName={}, pipelineId={}, type={}", appName, pipelineId, type, e);
        }
    }

    @Override
    public PageResponse<PipelineTriggerHistoryResponse> page(PipelineTriggerHistoryQueryRequest query) {
        if (query.getPipelineId() == null && !StringUtils.hasText(query.getAppName())) {
            throw new BusinessException(
                    PipelineTriggerHistoryConstants.MSG_TRIGGER_HISTORY_QUERY_PARAM_REQUIRED);
        }
        long pageNum = query.getPageNum() == null ? CommonConstants.DEFAULT_PAGE_NUM : query.getPageNum();
        long pageSize = query.getPageSize() == null ? CommonConstants.DEFAULT_PAGE_SIZE : query.getPageSize();
        IPage<PipelineTriggerHistory> pageResult = pipelineTriggerHistoryRepository.pageQuery(
                pageNum, pageSize, query.getPipelineId(), query.getAppName(),
                query.getStatus(), query.getType());
        List<PipelineTriggerHistoryResponse> records = pageResult.getRecords().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return PageResponse.of(records, pageResult.getTotal(), pageResult.getCurrent(),
                pageResult.getSize(), pageResult.getPages());
    }

    @Override
    public PipelineTriggerHistoryResponse getById(Long id) {
        PipelineTriggerHistory entity = pipelineTriggerHistoryRepository.selectById(id);
        if (entity == null) {
            throw new BusinessException(PipelineTriggerHistoryConstants.MSG_TRIGGER_HISTORY_NOT_EXIST);
        }
        return toResponse(entity);
    }

    private PipelineTriggerHistoryResponse toResponse(PipelineTriggerHistory entity) {
        if (entity == null) return null;
        PipelineTriggerHistoryResponse response = new PipelineTriggerHistoryResponse();
        BeanUtils.copyProperties(entity, response);
        return response;
    }
}
