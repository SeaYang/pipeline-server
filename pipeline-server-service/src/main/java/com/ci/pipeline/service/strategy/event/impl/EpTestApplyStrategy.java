package com.ci.pipeline.service.strategy.event.impl;

import com.ci.pipeline.common.constants.PipelineEventConstants;
import com.ci.pipeline.facade.request.PipelineEventTriggerRequest;
import com.ci.pipeline.facade.response.PipelineEventTriggerResponse;
import com.ci.pipeline.facade.response.PipelineEventTriggerResult;
import com.ci.pipeline.service.service.PipelineEventService;
import com.ci.pipeline.service.strategy.event.PipelineEventStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 效能平台提测事件策略。
 * <p>Bean 名称 "epTestApply" 与事件类型编码一致，由策略管理器自动路由。
 * <p>必填参数：app-name、git-branch
 */
@Slf4j
@Component(PipelineEventConstants.EVENT_TYPE_EP_TEST_APPLY)
public class EpTestApplyStrategy implements PipelineEventStrategy {

    @Autowired
    private PipelineEventService pipelineEventService;

    @Override
    public String eventType() {
        return PipelineEventConstants.EVENT_TYPE_EP_TEST_APPLY;
    }

    @Override
    public Object execute(PipelineEventTriggerRequest request) {
        List<PipelineEventTriggerResult> resultList = new ArrayList<>();

        List<Map<String, String>> paramList = request.getParamList();
        if (paramList == null || paramList.isEmpty()) {
            throw new IllegalArgumentException(PipelineEventConstants.MSG_PARAM_LIST_REQUIRED);
        }

        for (Map<String, String> param : paramList) {
            PipelineEventTriggerResult result = PipelineEventTriggerResult.builder()
                    .requestParams(param)
                    .build();
            try {
                // 校验必填参数
                String appName = param.get(PipelineEventConstants.PARAM_KEY_APP_NAME);
                String gitBranch = param.get(PipelineEventConstants.PARAM_KEY_GIT_BRANCH);
                if (!StringUtils.hasText(appName)) {
                    throw new IllegalArgumentException(PipelineEventConstants.MSG_APP_NAME_PARAM_REQUIRED);
                }
                if (!StringUtils.hasText(gitBranch)) {
                    throw new IllegalArgumentException(PipelineEventConstants.MSG_GIT_BRANCH_REQUIRED);
                }

                // 委托核心服务执行
                Long pipelineRunId = pipelineEventService.triggerAndExecute(
                        request.getEventType(), appName, param);
                result.setAppName(appName);
                result.setPipelineRunId(pipelineRunId);
            } catch (Exception e) {
                log.error("事件触发失败, eventType={}, param={}, error={}",
                        request.getEventType(), param, e.getMessage(), e);
                result.setAppName(param.get(PipelineEventConstants.PARAM_KEY_APP_NAME));
                result.setErrorMessage(e.getMessage());
            }
            resultList.add(result);
        }

        return PipelineEventTriggerResponse.builder()
                .resultList(resultList)
                .build();
    }
}
