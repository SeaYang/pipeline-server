package com.ci.pipeline.service.service.impl;

import com.ci.pipeline.common.auth.UserContext;
import com.ci.pipeline.common.constants.PipelineEventConstants;
import com.ci.pipeline.common.constants.PipelineTriggerHistoryConstants;
import com.ci.pipeline.common.exception.BusinessException;
import com.ci.pipeline.dao.entity.AppInfo;
import com.ci.pipeline.dao.entity.Pipeline;
import com.ci.pipeline.dao.entity.PipelineEventBind;
import com.ci.pipeline.dao.entity.PipelineTemplate;
import com.ci.pipeline.dao.entity.PipelineTemplateVersion;
import com.ci.pipeline.dao.repository.AppInfoRepository;
import com.ci.pipeline.dao.repository.PipelineRepository;
import com.ci.pipeline.dao.repository.PipelineTemplateRepository;
import com.ci.pipeline.dao.repository.PipelineTemplateVersionRepository;
import com.ci.pipeline.facade.request.PipelineExecuteRequest;
import com.ci.pipeline.facade.request.PipelineParametersRequest;
import com.ci.pipeline.facade.response.PipelineExecuteResponse;
import com.ci.pipeline.facade.response.PipelineRunParameterResponse;
import com.ci.pipeline.service.service.PipelineEventBindService;
import com.ci.pipeline.service.service.PipelineEventService;
import com.ci.pipeline.service.service.PipelineParameterService;
import com.ci.pipeline.service.service.PipelineService;
import com.ci.pipeline.service.service.PipelineTemplateEventBindService;
import com.ci.pipeline.service.service.PipelineTriggerHistoryService;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 流水线事件触发核心服务实现。
 * <p>负责：模板匹配 → pipeline 自动创建/复用 → 参数合并 → 执行流水线
 */
@Slf4j
@Service
public class PipelineEventServiceImpl implements PipelineEventService {

    @Autowired
    private PipelineTemplateEventBindService pipelineTemplateEventBindService;

    @Autowired
    private AppInfoRepository appInfoRepository;

    @Autowired
    private PipelineTemplateRepository pipelineTemplateRepository;

    @Autowired
    private PipelineTemplateVersionRepository pipelineTemplateVersionRepository;

    @Autowired
    private PipelineEventBindService pipelineEventBindService;

    @Autowired
    private PipelineRepository pipelineRepository;

    @Autowired
    private PipelineService pipelineService;

    @Autowired
    private PipelineParameterService pipelineParameterService;

    @Autowired
    private PipelineTriggerHistoryService pipelineTriggerHistoryService;

    @Override
    public Long triggerAndExecute(String eventType, String appName, Map<String, String> params) {
        Long pipelineRunId = null;
        String errorMessage = null;
        // 以下变量在流程中逐步赋值，用于 finally 记录触发历史
        String pipelineTemplateCode = null;
        String pipelineTemplateVersion = null;
        Long pipelineId = null;
        Long pipelineEventBindId = PipelineTriggerHistoryConstants.MANUAL_TRIGGER_ID;
        String creator = resolveEventCreator(eventType, params);

        try {
            // Step 1: 根据 eventType 查询绑定的模板编码列表
            List<String> templateCodes = pipelineTemplateEventBindService.listTemplateCodesByEventType(eventType);
            if (templateCodes == null || templateCodes.isEmpty()) {
                throw new BusinessException(String.format(
                        PipelineEventConstants.MSG_EVENT_NO_TEMPLATE_BIND, eventType));
            }

            // Step 2: 根据 appName 查询应用信息，获取编程语言
            AppInfo appInfo = appInfoRepository.selectByAppName(appName);
            if (appInfo == null) {
                throw new BusinessException(String.format(
                        PipelineEventConstants.MSG_APP_NOT_EXIST, appName));
            }
            String programmingLanguage = appInfo.getProgrammingLanguage();

            // Step 3: 根据编程语言过滤模板（pipeline_template_group = 编程语言）
            pipelineTemplateCode = matchTemplate(templateCodes, programmingLanguage, appName);

            // Step 4: 查询模板是否有生效版本
            PipelineTemplateVersion effectiveVersion =
                    pipelineTemplateVersionRepository.selectEffectiveByCode(pipelineTemplateCode);
            if (effectiveVersion == null) {
                throw new BusinessException(String.format(
                        PipelineEventConstants.MSG_TEMPLATE_NO_EFFECTIVE_VERSION, pipelineTemplateCode));
            }
            pipelineTemplateVersion = effectiveVersion.getVersion();

            // Step 5: 查询或创建 pipeline_event_bind
            PipelineEventBind eventBind = pipelineEventBindService.getByAppNameAndEventTypeAndTemplateCode(
                    appName, eventType, pipelineTemplateCode);
            if (eventBind != null) {
                // 已存在绑定，复用 pipelineId
                pipelineId = eventBind.getPipelineId();
                pipelineEventBindId = eventBind.getId();
            } else {
                // 不存在，创建 pipeline（name = appName-eventType）
                pipelineId = createPipelineForEvent(appName, eventType, pipelineTemplateCode);
                // 创建 pipeline_event_bind
                PipelineEventBind newBind = pipelineEventBindService.create(
                        appName, eventType, pipelineTemplateCode, pipelineId);
                pipelineEventBindId = newBind != null ? newBind.getId() : 0L;
            }

            // Step 6: 调用参数接口，获取完整的用户参数列表（API 传入值覆盖默认值）
            Map<String, String> executeParams = buildExecuteParams(pipelineId, params);

            // Step 7: 执行流水线
            PipelineExecuteRequest executeRequest = new PipelineExecuteRequest();
            executeRequest.setPipelineId(pipelineId);
            executeRequest.setParameters(executeParams);
            PipelineExecuteResponse response = pipelineService.execute(executeRequest);
            pipelineRunId = response != null ? response.getPipelineRunId() : null;

            return pipelineRunId;
        } catch (RuntimeException e) {
            errorMessage = e.getMessage();
            throw e;
        } finally {
            recordEventTriggerHistory(eventType, appName, params, pipelineId, pipelineRunId,
                    pipelineEventBindId, errorMessage, pipelineTemplateCode, pipelineTemplateVersion, creator);
        }
    }

    // ===== 私有方法 =====

    /**
     * 根据编程语言从模板编码列表中匹配唯一模板。
     * <p>遍历 templateCodes，查 pipeline_template，取 pipeline_template_group = programmingLanguage 的第一条。
     */
    private String matchTemplate(List<String> templateCodes, String programmingLanguage, String appName) {
        for (String code : templateCodes) {
            PipelineTemplate template = pipelineTemplateRepository.selectByPipelineTemplateCode(code);
            if (template != null && programmingLanguage != null
                    && programmingLanguage.equals(template.getPipelineTemplateGroup())) {
                return code;
            }
        }
        throw new BusinessException(String.format(
                PipelineEventConstants.MSG_NO_MATCHED_TEMPLATE, appName, programmingLanguage));
    }

    /**
     * 为事件触发创建 pipeline 实例。
     * <p>name 规则：appName-eventType，如 go-web-demo-epTestApply。
     * <p>事件触发接口无认证，UserContext 可能为空，creator 使用系统标识兜底。
     */
    private Long createPipelineForEvent(String appName, String eventType, String pipelineTemplateCode) {
        Pipeline entity = new Pipeline();
        entity.setAppName(appName);
        entity.setPipelineTemplateCode(pipelineTemplateCode);
        entity.setName(appName + "-" + eventType);
        String creator = UserContext.getUserId();
        entity.setCreator(creator != null ? creator : PipelineEventConstants.EVENT_SYSTEM);
        pipelineRepository.insert(entity);
        log.info("事件触发自动创建pipeline, appName={}, eventType={}, templateCode={}, pipelineId={}",
                appName, eventType, pipelineTemplateCode, entity.getId());
        return entity.getId();
    }

    /**
     * 构建执行参数：调用 listRunParameters 获取完整参数列表（含默认值），API 传入值覆盖默认值。
     */
    private Map<String, String> buildExecuteParams(Long pipelineId, Map<String, String> apiParams) {
        PipelineParametersRequest parametersRequest = new PipelineParametersRequest();
        parametersRequest.setPipelineId(pipelineId);
        parametersRequest.setCurrentValues(apiParams);

        List<PipelineRunParameterResponse> runParameters =
                pipelineParameterService.listRunParameters(parametersRequest);

        Map<String, String> executeParams = new HashMap<>();
        if (runParameters != null) {
            for (PipelineRunParameterResponse param : runParameters) {
                if (StringUtils.hasText(param.getName())) {
                    executeParams.put(param.getName(), param.getValue());
                }
            }
        }
        return executeParams;
    }

    /**
     * 解析事件触发的 creator：优先取 operator 参数，无则取 eventType
     */
    private String resolveEventCreator(String eventType, Map<String, String> params) {
        if (params != null) {
            String operator = params.get("operator");
            if (StringUtils.hasText(operator)) {
                return operator;
            }
        }
        return eventType;
    }

    /**
     * 记录事件触发历史
     */
    private void recordEventTriggerHistory(String eventType, String appName, Map<String, String> params,
                                           Long pipelineId, Long pipelineRunId, Long pipelineEventBindId,
                                           String errorMessage, String pipelineTemplateCode,
                                           String pipelineTemplateVersion, String creator) {
        try {
            if (pipelineId == null) {
                // pipelineId 为 null 说明在执行之前就失败了（如模板匹配失败），
                // 此时 pipelineId 未知，使用 0 占位
                pipelineId = 0L;
            }
            String status = pipelineRunId != null
                    ? PipelineTriggerHistoryConstants.STATUS_SUCCESS
                    : PipelineTriggerHistoryConstants.STATUS_FAILED;
            String requestBody = params != null ? JSON.toJSONString(params) : null;

            pipelineTriggerHistoryService.add(
                    appName,
                    pipelineId,
                    pipelineRunId,
                    pipelineEventBindId,
                    status,
                    eventType,
                    creator,
                    requestBody,
                    errorMessage,
                    pipelineTemplateCode,
                    pipelineTemplateVersion);
        } catch (Exception e) {
            log.error("记录事件触发历史失败, eventType={}, appName={}", eventType, appName, e);
        }
    }
}
