package com.ci.pipeline.service.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ci.pipeline.common.auth.UserContext;
import com.ci.pipeline.common.constants.CommonConstants;
import com.ci.pipeline.common.constants.PipelineConstants;
import com.ci.pipeline.common.constants.PipelineConcurrencyConstants;
import com.ci.pipeline.common.enums.OverLimitPolicyEnum;
import com.ci.pipeline.common.constants.PipelineParameterConstants;
import com.ci.pipeline.common.constants.PipelineTriggerHistoryConstants;
import com.ci.pipeline.common.enums.ParamTypeEnum;
import com.ci.pipeline.common.exception.BusinessException;
import com.ci.pipeline.common.util.SortUtil;
import com.ci.pipeline.dao.entity.AppInfo;
import com.ci.pipeline.dao.entity.Pipeline;
import com.ci.pipeline.dao.entity.PipelineTemplate;
import com.ci.pipeline.dao.entity.PipelineTemplateVersion;
import com.ci.pipeline.dao.entity.PipelineParameter;
import com.ci.pipeline.dao.repository.AppInfoRepository;
import com.ci.pipeline.dao.repository.PipelineParameterRepository;
import com.ci.pipeline.dao.repository.PipelineRepository;
import com.ci.pipeline.dao.repository.PipelineTemplateRepository;
import com.ci.pipeline.dao.repository.PipelineTemplateVersionRepository;
import com.ci.pipeline.facade.request.PipelineCreateRequest;
import com.ci.pipeline.facade.request.PipelineExecuteRequest;
import com.ci.pipeline.facade.request.PipelineQueryRequest;
import com.ci.pipeline.facade.request.PipelineUpdateRequest;
import com.ci.pipeline.facade.response.PageResponse;
import com.ci.pipeline.facade.response.PipelineExecuteResponse;
import com.ci.pipeline.facade.response.PipelineResponse;
import com.ci.pipeline.facade.response.PipelineTemplateOptionResponse;
import com.ci.pipeline.service.concurrency.PipelineConcurrencyChecker;
import com.ci.pipeline.service.scheduler.cluster.ClusterScheduleStrategyManager;
import com.ci.pipeline.service.service.ClusterConfigService;
import com.ci.pipeline.service.remote.ArgoWorkflowAgent;
import com.ci.pipeline.service.service.PipelineRunService;
import com.ci.pipeline.service.service.PipelineService;
import com.ci.pipeline.service.service.PipelineTriggerHistoryService;
import com.ci.pipeline.service.strategy.pipeline.parameter.PipelineParameterStrategyManager;
import com.ci.pipeline.service.strategy.ParamResolveContext;
import com.ci.pipeline.service.util.ArgoWorkflowUtil;
import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.argoproj.workflow.models.IoArgoprojWorkflowV1alpha1Workflow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 流水线实例业务实现
 */
@Slf4j
@Service
public class PipelineServiceImpl implements PipelineService {

    /**
     * 分页排序字段白名单：出参字段名（camelCase） → 数据库列名（snake_case）
     */
    private static final Map<String, String> SORT_FIELD_MAP;
    static {
        Map<String, String> m = new HashMap<>();
        m.put("id", "id");
        m.put("name", "name");
        m.put("appName", "app_name");
        m.put("pipelineTemplateCode", "pipeline_template_code");
        m.put("creator", "creator");
        m.put("createTime", "create_time");
        m.put("updateTime", "update_time");
        SORT_FIELD_MAP = Collections.unmodifiableMap(m);
    }

    /** 共享 ObjectMapper 实例（ArgoWorkflowUtil 参数解析用） */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private PipelineRepository pipelineRepository;

    @Autowired
    private AppInfoRepository appInfoRepository;

    @Autowired
    private PipelineTemplateRepository pipelineTemplateRepository;

    @Autowired
    private PipelineTemplateVersionRepository pipelineTemplateVersionRepository;

    @Autowired
    private ArgoWorkflowAgent argoWorkflowAgent;

    @Autowired
    private ClusterConfigService clusterConfigService;

    @Autowired
    private ClusterScheduleStrategyManager clusterScheduleStrategyManager;

    @Autowired
    private PipelineRunService pipelineRunService;

    @Autowired
    private PipelineParameterRepository pipelineParameterRepository;

    @Autowired
    private PipelineParameterStrategyManager pipelineParameterStrategyManager;

    @Autowired
    private PipelineTriggerHistoryService pipelineTriggerHistoryService;

    @Autowired
    private PipelineConcurrencyChecker pipelineConcurrencyChecker;

    @Override
    public PipelineResponse create(PipelineCreateRequest request) {
        validateCreateRequired(request);
        // appName 必须存在于 app_info
        if (appInfoRepository.selectByAppName(request.getAppName()) == null) {
            throw new BusinessException(String.format(
                    PipelineConstants.MSG_APP_NOT_EXIST, request.getAppName()));
        }
        // 流水线模板编码必须存在于 pipeline_template
        if (pipelineTemplateRepository.selectByPipelineTemplateCode(request.getPipelineTemplateCode()) == null) {
            throw new BusinessException(String.format(
                    PipelineConstants.MSG_TEMPLATE_NOT_EXIST, request.getPipelineTemplateCode()));
        }
        Pipeline entity = new Pipeline();
        BeanUtils.copyProperties(request, entity);
        // 创建人取当前登录用户（Controller 已 @RequireLogin，保证非空）
        entity.setCreator(UserContext.getUserId());
        pipelineRepository.insert(entity);
        log.info("新增流水线成功, appName={}, pipelineTemplateCode={}, id={}",
                entity.getAppName(), entity.getPipelineTemplateCode(), entity.getId());
        return toResponse(pipelineRepository.selectById(entity.getId()));
    }

    @Override
    public PipelineResponse update(PipelineUpdateRequest request) {
        if (request.getId() == null) {
            throw new BusinessException(PipelineConstants.MSG_PIPELINE_ID_REQUIRED);
        }
        Pipeline existing = pipelineRepository.selectById(request.getId());
        if (existing == null) {
            throw new BusinessException(PipelineConstants.MSG_PIPELINE_NOT_EXIST);
        }
        // 若传入 name，需校验非空
        if (request.getName() != null && !StringUtils.hasText(request.getName())) {
            throw new BusinessException(PipelineConstants.MSG_PIPELINE_NAME_REQUIRED);
        }
        // 并发控制字段校验：maxRunningLimit 可空，非空时 ≥1（不校验上限，clamp 在执行时生效）；overLimitPolicy 可空，非空时枚举校验
        validateConcurrencyFields(request.getMaxRunningLimit(), request.getOverLimitPolicy());
        // 仅允许修改 name / maxRunningLimit / overLimitPolicy（creator / appName / pipelineTemplateCode 由系统维护，不可改）
        Pipeline entity = new Pipeline();
        entity.setId(request.getId());
        entity.setName(request.getName());
        entity.setMaxRunningLimit(request.getMaxRunningLimit());
        entity.setOverLimitPolicy(request.getOverLimitPolicy());
        pipelineRepository.updateById(entity);
        log.info("修改流水线成功, id={}", request.getId());
        return toResponse(pipelineRepository.selectById(request.getId()));
    }

    @Override
    public void deleteById(Long id) {
        Pipeline existing = pipelineRepository.selectById(id);
        if (existing == null) {
            throw new BusinessException(PipelineConstants.MSG_PIPELINE_NOT_EXIST);
        }
        pipelineRepository.deleteById(id);
        log.info("删除流水线成功, id={}, appName={}", id, existing.getAppName());
    }

    @Override
    public PipelineResponse getById(Long id) {
        Pipeline entity = pipelineRepository.selectById(id);
        if (entity == null) {
            throw new BusinessException(PipelineConstants.MSG_PIPELINE_NOT_EXIST);
        }
        return toResponse(entity);
    }

    @Override
    public PageResponse<PipelineResponse> page(PipelineQueryRequest query) {
        long pageNum = query.getPageNum() == null ? CommonConstants.DEFAULT_PAGE_NUM : query.getPageNum();
        long pageSize = query.getPageSize() == null ? CommonConstants.DEFAULT_PAGE_SIZE : query.getPageSize();
        // 解析排序字段（白名单映射）与方向（默认 desc）；sortField 为空时走默认排序（创建时间倒序）
        String sortField = SortUtil.resolveField(query.getSortField(), SORT_FIELD_MAP);
        String sortOrder = sortField != null ? SortUtil.resolveOrder(query.getSortOrder()) : null;
        IPage<Pipeline> pageResult = pipelineRepository.pageQuery(
                pageNum, pageSize, query.getAppName(), sortField, sortOrder);
        List<PipelineResponse> records = pageResult.getRecords().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return PageResponse.of(records, pageResult.getTotal(), pageResult.getCurrent(), pageResult.getSize(), pageResult.getPages());
    }

    @Override
    public List<PipelineTemplateOptionResponse> listTemplates(String appName) {
        if (!StringUtils.hasText(appName)) {
            throw new BusinessException(PipelineConstants.MSG_APP_NAME_REQUIRED);
        }
        // 取 app 所属编程语言
        AppInfo appInfo = appInfoRepository.selectByAppName(appName);
        if (appInfo == null) {
            throw new BusinessException(String.format(PipelineConstants.MSG_APP_NOT_EXIST, appName));
        }
        String programmingLanguage = appInfo.getProgrammingLanguage();
        // 按编程语言分组取模板列表（pipeline_template_group 当前即填编程语言）
        List<PipelineTemplate> templates = pipelineTemplateRepository.listQuery(programmingLanguage, null, null);
        List<PipelineTemplateOptionResponse> result = new ArrayList<>();
        for (PipelineTemplate template : templates) {
            // 仅保留存在生效中版本的模板，模板详情取自生效版本
            PipelineTemplateVersion effective =
                    pipelineTemplateVersionRepository.selectEffectiveByCode(template.getPipelineTemplateCode());
            if (effective == null) {
                continue;
            }
            PipelineTemplateOptionResponse option = new PipelineTemplateOptionResponse();
            option.setPipelineTemplateCode(template.getPipelineTemplateCode());
            option.setName(template.getName());
            option.setDescription(template.getDescription());
            option.setTemplateDetail(effective.getTemplateDetail());
            result.add(option);
        }
        return result;
    }

    @Override
    public PipelineExecuteResponse execute(PipelineExecuteRequest request) {
        if (request == null || request.getPipelineId() == null) {
            throw new BusinessException(PipelineConstants.MSG_PIPELINE_ID_REQUIRED);
        }
        Pipeline pipeline = pipelineRepository.selectById(request.getPipelineId());
        if (pipeline == null) {
            throw new BusinessException(PipelineConstants.MSG_PIPELINE_NOT_EXIST);
        }
        // 生效中版本必须存在（模板已同步到 argo），否则禁止执行
        PipelineTemplateVersion effective =
                pipelineTemplateVersionRepository.selectEffectiveByCode(pipeline.getPipelineTemplateCode());
        if (effective == null) {
            throw new BusinessException(String.format(
                    PipelineConstants.MSG_TEMPLATE_NO_EFFECTIVE_VERSION, pipeline.getPipelineTemplateCode()));
        }
        // 参数对象转 Argo submitOpts 的 name=value 列表
        // 1. 合并 user 参数 + system 参数（自动填充）
        // 2. needSystemProcess = true 的参数做值映射转换（value → realValue）
        // 3. required + regex 校验
        Map<String, String> finalParameters = buildAndValidateParameters(pipeline, effective, request.getParameters());
        List<String> paramList = toArgoParameters(finalParameters);
        // ★ 多集群调度：按模板的调度策略 + 实时打分选择执行集群
        PipelineTemplate template = pipelineTemplateRepository.selectByPipelineTemplateCode(pipeline.getPipelineTemplateCode());
        // ★ 三层并发检查（L1 全局 → L2 应用×模板 → L3 流水线）：插在参数校验之后、集群选择之前，
        //   避免额度检查通过后又被参数校验拦下，浪费 ReplaceOldest 已终止的执行
        pipelineConcurrencyChecker.checkBeforeExecute(pipeline, template);
        String clusterName = clusterScheduleStrategyManager
                .getStrategy(template != null ? template.getClusterSchedulePolicy() : null)
                .selectCluster(template);
        // 模板名 = pipelineTemplateCode（版本生效时已强制 metadata.name 与编码一致）
        IoArgoprojWorkflowV1alpha1Workflow workflow;
        try {
            workflow = argoWorkflowAgent.submitWorkflowByTemplate(
                    clusterName, clusterConfigService.getNamespace(clusterName),
                    pipeline.getPipelineTemplateCode(), paramList);
        } catch (RuntimeException e) {
            log.error("执行流水线失败, pipelineId={}, pipelineTemplateCode={}, clusterName={}",
                    pipeline.getId(), pipeline.getPipelineTemplateCode(), clusterName, e);
            throw new BusinessException(String.format(PipelineConstants.MSG_EXECUTE_FAILED, e.getMessage()));
        }
        String workflowName = workflow.getMetadata() != null ? workflow.getMetadata().getName() : null;
        log.info("执行流水线成功, pipelineId={}, workflowName={}, clusterName={}",
                pipeline.getId(), workflowName, clusterName);
        // 触发 Argo 成功后落地执行记录（含执行集群），并触发异步状态同步；commitId / gitBranch 暂不维护
        Long pipelineRunId = pipelineRunService.createRun(
                pipeline, effective, workflow, finalParameters, clusterName);
        return new PipelineExecuteResponse(pipelineRunId, workflowName);
    }

    @Override
    public PipelineExecuteResponse executeWithHistory(PipelineExecuteRequest request) {
        Long pipelineRunId = null;
        String errorMessage = null;
        Pipeline pipeline = null;
        PipelineTemplateVersion effective = null;
        try {
            // 先查出 pipeline 和模板信息，用于记录触发历史
            if (request != null && request.getPipelineId() != null) {
                pipeline = pipelineRepository.selectById(request.getPipelineId());
                if (pipeline != null) {
                    effective = pipelineTemplateVersionRepository.selectEffectiveByCode(
                            pipeline.getPipelineTemplateCode());
                }
            }
            PipelineExecuteResponse response = execute(request);
            pipelineRunId = response != null ? response.getPipelineRunId() : null;
            return response;
        } catch (RuntimeException e) {
            errorMessage = e.getMessage();
            throw e;
        } finally {
            recordManualTriggerHistory(request, pipeline, effective, pipelineRunId, errorMessage);
        }
    }

    /**
     * 记录手动触发历史
     */
    private void recordManualTriggerHistory(PipelineExecuteRequest request, Pipeline pipeline,
                                            PipelineTemplateVersion effective,
                                            Long pipelineRunId, String errorMessage) {
        try {
            if (pipeline == null) return;
            String status = pipelineRunId != null
                    ? PipelineTriggerHistoryConstants.STATUS_SUCCESS
                    : PipelineTriggerHistoryConstants.STATUS_FAILED;
            String requestBody = request != null ? JSON.toJSONString(request) : null;
            String templateCode = pipeline.getPipelineTemplateCode();
            String templateVersion = effective != null ? effective.getVersion() : null;
            String creator = UserContext.getUserId();

            pipelineTriggerHistoryService.add(
                    pipeline.getAppName(),
                    pipeline.getId(),
                    pipelineRunId,
                    PipelineTriggerHistoryConstants.MANUAL_TRIGGER_ID,
                    status,
                    PipelineTriggerHistoryConstants.TRIGGER_TYPE_USER,
                    creator,
                    requestBody,
                    errorMessage,
                    templateCode,
                    templateVersion);
        } catch (Exception e) {
            log.error("记录手动触发历史失败", e);
        }
    }

    // ===== 私有工具方法 =====

    /**
     * 新增必填字段校验
     */
    private void validateCreateRequired(PipelineCreateRequest request) {
        if (!StringUtils.hasText(request.getName())) {
            throw new BusinessException(PipelineConstants.MSG_PIPELINE_NAME_REQUIRED);
        }
        if (!StringUtils.hasText(request.getAppName())) {
            throw new BusinessException(PipelineConstants.MSG_APP_NAME_REQUIRED);
        }
        if (!StringUtils.hasText(request.getPipelineTemplateCode())) {
            throw new BusinessException(PipelineConstants.MSG_PIPELINE_TEMPLATE_CODE_REQUIRED);
        }
    }

    /**
     * 构建完整参数 Map（user + system），执行值映射转换和参数校验。
     * <p>
     * 流程：
     * 1. 查模板参数名列表 + 参数定义
     * 2. 合并 user 参数（来自 request）和自动计算参数（策略管理器按参数名路由）
     * 3. 对 needSystemProcess = true 的参数做值映射转换（option_config 中 value → realValue）
     * 4. required 非空校验 + regexPattern 正则校验
     *
     * @param pipeline         流水线实例
     * @param effective        生效模板版本
     * @param userParameters   用户填写的参数 Map
     * @return 转换+校验后的完整参数 Map
     */
    private Map<String, String> buildAndValidateParameters(Pipeline pipeline,
                                                           PipelineTemplateVersion effective,
                                                           Map<String, String> userParameters) {
        // 解析模板参数名列表
        List<String> templateParamNames = ArgoWorkflowUtil.extractParamNames(
                effective.getTemplateDetail(), OBJECT_MAPPER);
        // 查参数定义
        List<PipelineParameter> definedParams = pipelineParameterRepository.listByNames(templateParamNames);
        Map<String, PipelineParameter> definedMap = new HashMap<>();
        for (PipelineParameter p : definedParams) {
            definedMap.put(p.getName(), p);
        }

        // 构建上下文
        Map<String, String> resolvedValues = new HashMap<>();
        ParamResolveContext context = ParamResolveContext.builder()
                .pipelineId(pipeline.getId())
                .appName(pipeline.getAppName())
                .pipelineTemplateCode(pipeline.getPipelineTemplateCode())
                .resolvedValues(resolvedValues)
                .build();

        // 合并参数：user 参数取用户提交值，system 参数走策略管理器自动计算
        Map<String, String> finalParameters = new HashMap<>();
        for (String paramName : templateParamNames) {
            PipelineParameter paramDef = definedMap.get(paramName);
            if (paramDef == null) {
                // 未定义参数降级：取用户值，无用户值时尝试从模板 default 兜底
                String value = userParameters != null ? userParameters.get(paramName) : null;
                if (value == null) {
                    value = ArgoWorkflowUtil.extractParamDefault(
                            effective.getTemplateDetail(), paramName, OBJECT_MAPPER);
                }
                if (value != null) {
                    finalParameters.put(paramName, value);
                }
                log.warn("模板参数未在参数定义表中配置, 降级为普通参数, paramName={}, pipelineId={}",
                        paramName, pipeline.getId());
                continue;
            }
            if (ParamTypeEnum.SYSTEM.getCode().equals(paramDef.getParamType())) {
                // system 参数：走策略管理器自动计算
                String value = pipelineParameterStrategyManager
                        .getStrategy(paramName).buildParameter(paramDef, context);
                if (value == null) {
                    value = paramDef.getDefaultValue();
                }
                if (value != null) {
                    finalParameters.put(paramName, value);
                } else {
                    log.warn("系统参数填充失败且无默认值, paramName={}, pipelineId={}", paramName, pipeline.getId());
                }
            } else {
                // user 参数：取用户提交值
                String value = userParameters != null ? userParameters.get(paramName) : null;
                if (value != null) {
                    finalParameters.put(paramName, value);
                }
            }
        }

        // 过滤未知参数（用户传入但模板未声明的），记录告警
        if (userParameters != null) {
            for (String userParamName : userParameters.keySet()) {
                if (!templateParamNames.contains(userParamName)) {
                    log.warn("用户传入的参数未在模板中声明, 已忽略, paramName={}, pipelineId={}",
                            userParamName, pipeline.getId());
                }
            }
        }

        // 系统内部处理（值映射转换 + 个别参数自定义处理）+ 校验
        for (String paramName : templateParamNames) {
            PipelineParameter paramDef = definedMap.get(paramName);
            String value = finalParameters.get(paramName);
            if (paramDef == null) {
                continue;
            }
            // 系统处理：走策略类的 systemProcess（默认做值映射转换，个别参数可自定义）
            if (value != null) {
                String handledValue = pipelineParameterStrategyManager
                        .getStrategy(paramName).systemProcess(paramDef, value);
                finalParameters.put(paramName, handledValue);
                value = handledValue;
            }
            // required 校验
            if (Boolean.TRUE.equals(paramDef.getRequired()) && (value == null || value.trim().isEmpty())) {
                throw new BusinessException(String.format(
                        PipelineParameterConstants.MSG_PARAM_REQUIRED,
                        paramDef.getLabel() != null ? paramDef.getLabel() : paramName));
            }
            // regex 校验
            if (value != null && StringUtils.hasText(paramDef.getRegexPattern())) {
                if (!value.matches(paramDef.getRegexPattern())) {
                    throw new BusinessException(String.format(
                            PipelineParameterConstants.MSG_PARAM_REGEX_FAIL,
                            paramDef.getLabel() != null ? paramDef.getLabel() : paramName));
                }
            }
        }
        return finalParameters;
    }

    /**
     * 将参数 Map 转为 Argo submitOpts 的 {@code name=value} 字符串列表（跳过 null 值）。
     */
    private List<String> toArgoParameters(Map<String, String> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>(parameters.size());
        parameters.forEach((name, value) -> {
            if (name != null && value != null) {
                result.add(name + "=" + value);
            }
        });
        return result;
    }

    private PipelineResponse toResponse(Pipeline entity) {
        if (entity == null) {
            return null;
        }
        PipelineResponse response = new PipelineResponse();
        BeanUtils.copyProperties(entity, response);
        // 回显生效并发上限（clamp 后），便于前端展示
        PipelineTemplate template = pipelineTemplateRepository
                .selectByPipelineTemplateCode(entity.getPipelineTemplateCode());
        response.setEffectiveMaxRunningLimit(
                pipelineConcurrencyChecker.resolveEffectiveLimit(entity, template));
        return response;
    }

    /**
     * 并发控制字段校验：maxRunningLimit 可空，非空时 ≥1（不校验上限，clamp 在执行时生效）；
     * overLimitPolicy 可空，非空时枚举校验。
     */
    private void validateConcurrencyFields(Integer maxRunningLimit, String overLimitPolicy) {
        if (maxRunningLimit != null && maxRunningLimit < 1) {
            throw new BusinessException(String.format(
                    PipelineConcurrencyConstants.MSG_PIPELINE_LIMIT_INVALID, maxRunningLimit));
        }
        if (StringUtils.hasText(overLimitPolicy)
                && !OverLimitPolicyEnum.isValidCode(overLimitPolicy)) {
            throw new BusinessException(String.format(
                    PipelineConcurrencyConstants.MSG_OVER_LIMIT_POLICY_INVALID, overLimitPolicy));
        }
    }
}
