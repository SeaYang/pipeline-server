package com.ci.pipeline.service.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ci.pipeline.common.auth.UserContext;
import com.ci.pipeline.common.constants.CommonConstants;
import com.ci.pipeline.common.constants.PipelineConstants;
import com.ci.pipeline.common.constants.PipelineParameterConstants;
import com.ci.pipeline.common.enums.ComponentTypeEnum;
import com.ci.pipeline.common.enums.DefaultValueStrategyTypeEnum;
import com.ci.pipeline.common.enums.ParamTypeEnum;
import com.ci.pipeline.common.exception.BusinessException;
import com.ci.pipeline.common.util.SortUtil;
import com.ci.pipeline.dao.entity.Pipeline;
import com.ci.pipeline.dao.entity.PipelineParameter;
import com.ci.pipeline.dao.entity.PipelineTemplateVersion;
import com.ci.pipeline.dao.repository.PipelineParameterRepository;
import com.ci.pipeline.dao.repository.PipelineRepository;
import com.ci.pipeline.dao.repository.PipelineTemplateVersionRepository;
import com.ci.pipeline.facade.request.PipelineParameterCreateRequest;
import com.ci.pipeline.facade.request.PipelineParameterQueryRequest;
import com.ci.pipeline.facade.request.PipelineParameterUpdateRequest;
import com.ci.pipeline.facade.request.PipelineParametersRefreshRequest;
import com.ci.pipeline.facade.request.PipelineParametersRequest;
import com.ci.pipeline.facade.response.PageResponse;
import com.ci.pipeline.facade.response.PipelineParameterResponse;
import com.ci.pipeline.facade.response.PipelineRunParameterResponse;
import com.ci.pipeline.service.service.PipelineParameterService;
import com.ci.pipeline.service.strategy.pipeline.parameter.PipelineParameterStrategyManager;
import com.ci.pipeline.service.strategy.ParamResolveContext;
import com.ci.pipeline.service.util.ArgoWorkflowUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
@Slf4j
@Service
public class PipelineParameterServiceImpl implements PipelineParameterService {

    private static final Pattern NAME_PATTERN = Pattern.compile(PipelineParameterConstants.NAME_REGEX);

    /** 共享 ObjectMapper 实例（ArgoWorkflowUtil 参数解析用） */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 分页排序字段白名单：出参字段名（camelCase） → 数据库列名（snake_case）
     */
    private static final Map<String, String> SORT_FIELD_MAP;
    static {
        Map<String, String> m = new HashMap<>();
        m.put("id", "id");
        m.put("name", "name");
        m.put("label", "label");
        m.put("paramType", "param_type");
        m.put("componentType", "component_type");
        m.put("required", "required");
        m.put("needSystemProcess", "need_system_process");
        m.put("paramGroup", "param_group");
        m.put("paramGroupSort", "param_group_sort");
        m.put("creator", "creator");
        m.put("createTime", "create_time");
        m.put("updateTime", "update_time");
        SORT_FIELD_MAP = SortUtil.unmodifiableWhitelist(m);
    }

    @Autowired
    private PipelineParameterRepository pipelineParameterRepository;

    @Autowired
    private PipelineRepository pipelineRepository;

    @Autowired
    private PipelineTemplateVersionRepository pipelineTemplateVersionRepository;

    @Autowired
    private PipelineParameterStrategyManager pipelineParameterStrategyManager;

    // ============================== CRUD ==============================

    @Override
    public PipelineParameterResponse create(PipelineParameterCreateRequest request) {
        validateRequiredFields(request.getName(), request.getLabel(), request.getParamType(), request.getParamGroup());
        validateNameFormat(request.getName());
        validateEnumFields(request.getComponentType(), request.getParamType());
        validateJsonFields(request.getOptionConfig(), request.getDefaultValueStrategyConfig(), request.getDependParams());

        if (pipelineParameterRepository.countByName(request.getName(), null) > 0) {
            throw new BusinessException(String.format(
                    PipelineParameterConstants.MSG_NAME_DUPLICATED, request.getName()));
        }

        validateDependParams(request.getDependParams(), null, request.getName());

        PipelineParameter entity = new PipelineParameter();
        BeanUtils.copyProperties(request, entity);
        applyDefaults(entity);
        entity.setCreator(UserContext.getUserId());
        pipelineParameterRepository.insert(entity);
        log.info("新增参数定义成功, name={}, id={}", entity.getName(), entity.getId());
        return toResponse(pipelineParameterRepository.selectById(entity.getId()));
    }

    @Override
    public PipelineParameterResponse update(PipelineParameterUpdateRequest request) {
        if (request.getId() == null) {
            throw new BusinessException(PipelineParameterConstants.MSG_ID_REQUIRED);
        }
        PipelineParameter existing = pipelineParameterRepository.selectById(request.getId());
        if (existing == null) {
            throw new BusinessException(PipelineParameterConstants.MSG_PARAM_NOT_EXIST);
        }
        validateRequiredFields(request.getName(), request.getLabel(), request.getParamType(), request.getParamGroup());
        validateNameFormat(request.getName());
        validateEnumFields(request.getComponentType(), request.getParamType());
        validateJsonFields(request.getOptionConfig(), request.getDefaultValueStrategyConfig(), request.getDependParams());

        if (pipelineParameterRepository.countByName(request.getName(), request.getId()) > 0) {
            throw new BusinessException(String.format(
                    PipelineParameterConstants.MSG_NAME_DUPLICATED, request.getName()));
        }

        validateDependParams(request.getDependParams(), request.getId(), request.getName());

        BeanUtils.copyProperties(request, existing);
        applyDefaults(existing);
        pipelineParameterRepository.updateById(existing);
        log.info("更新参数定义成功, name={}, id={}", existing.getName(), existing.getId());
        return toResponse(pipelineParameterRepository.selectById(existing.getId()));
    }

    @Override
    public void deleteById(Long id) {
        PipelineParameter existing = pipelineParameterRepository.selectById(id);
        if (existing == null) {
            throw new BusinessException(PipelineParameterConstants.MSG_PARAM_NOT_EXIST);
        }
        pipelineParameterRepository.deleteById(id);
        log.info("删除参数定义成功, name={}, id={}", existing.getName(), id);
    }

    @Override
    public PipelineParameterResponse getById(Long id) {
        PipelineParameter entity = pipelineParameterRepository.selectById(id);
        if (entity == null) {
            throw new BusinessException(PipelineParameterConstants.MSG_PARAM_NOT_EXIST);
        }
        return toResponse(entity);
    }

    @Override
    public PipelineParameterResponse getByName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new BusinessException(PipelineParameterConstants.MSG_NAME_REQUIRED);
        }
        PipelineParameter entity = pipelineParameterRepository.selectByName(name);
        if (entity == null) {
            throw new BusinessException(PipelineParameterConstants.MSG_PARAM_NOT_EXIST);
        }
        return toResponse(entity);
    }

    @Override
    public PageResponse<PipelineParameterResponse> page(PipelineParameterQueryRequest query) {
        long pageNum = query.getPageNum() == null ? CommonConstants.DEFAULT_PAGE_NUM : query.getPageNum();
        long pageSize = query.getPageSize() == null ? CommonConstants.DEFAULT_PAGE_SIZE : query.getPageSize();
        String sortField = SortUtil.resolveField(query.getSortField(), SORT_FIELD_MAP);
        String sortOrder = sortField != null ? SortUtil.resolveOrder(query.getSortOrder()) : null;

        IPage<PipelineParameter> pageResult = pipelineParameterRepository.pageQuery(
                pageNum, pageSize, query.getName(), query.getLabel(),
                query.getParamType(), query.getParamGroup(), sortField, sortOrder);
        List<PipelineParameterResponse> records = pageResult.getRecords().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return PageResponse.of(records, pageResult.getTotal(), pageResult.getCurrent(),
                pageResult.getSize(), pageResult.getPages());
    }

    @Override
    public List<PipelineParameterResponse> listAllSimple() {
        return pipelineParameterRepository.listAllSimple().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ============================== 参数解析引擎 ==============================

    @Override
    public List<PipelineRunParameterResponse> listRunParameters(PipelineParametersRequest request) {
        if (request == null || request.getPipelineId() == null) {
            throw new BusinessException(PipelineConstants.MSG_PIPELINE_ID_REQUIRED);
        }
        Long pipelineId = request.getPipelineId();
        // 1. 查 pipeline → 获取 appName、pipelineTemplateCode
        Pipeline pipeline = pipelineRepository.selectById(pipelineId);
        if (pipeline == null) {
            throw new BusinessException(PipelineConstants.MSG_PIPELINE_NOT_EXIST);
        }
        // 2. 查生效模板版本 → 解析 spec.arguments.parameters 得到参数名列表
        PipelineTemplateVersion effective =
                pipelineTemplateVersionRepository.selectEffectiveByCode(pipeline.getPipelineTemplateCode());
        if (effective == null) {
            throw new BusinessException(String.format(
                    PipelineConstants.MSG_TEMPLATE_NO_EFFECTIVE_VERSION, pipeline.getPipelineTemplateCode()));
        }
        List<String> templateParamNames = ArgoWorkflowUtil.extractParamNames(
                effective.getTemplateDetail(), OBJECT_MAPPER);
        if (templateParamNames.isEmpty()) {
            return Collections.emptyList();
        }
        // 3. 递归加载参数定义（模板参数 + 所有层级的依赖参数），未定义参数直接抛异常
        List<PipelineParameter> allParams = loadParameterDefinitions(templateParamNames);

        // 4. 构建参数依赖图，拓扑排序（按 depend_params）
        List<PipelineParameter> sortedParams = topologicalSort(allParams);

        // 5. 构建参数计算上下文，合并前端/第三方传入的已有值
        Map<String, String> resolvedValues = new HashMap<>();
        if (request.getCurrentValues() != null) {
            resolvedValues.putAll(request.getCurrentValues());
        }
        ParamResolveContext context = ParamResolveContext.builder()
                .pipelineId(pipeline.getId())
                .appName(pipeline.getAppName())
                .pipelineTemplateCode(pipeline.getPipelineTemplateCode())
                .resolvedValues(resolvedValues)
                .build();

        // 6. 按拓扑顺序逐个计算参数初始值（已有值的跳过，不覆盖）
        for (PipelineParameter param : sortedParams) {
            if (resolvedValues.containsKey(param.getName())) {
                continue;
            }
            String value = resolveParamValue(param, context);
            if (value != null) {
                resolvedValues.put(param.getName(), value);
            }
        }

        // 7. 过滤掉 system 参数，只返回 user 参数
        // 8. 按 param_group + param_group_sort 排序返回
        return sortedParams.stream()
                .filter(p -> !ParamTypeEnum.SYSTEM.getCode().equals(p.getParamType()))
                .map(p -> toRunParameterResponse(p, resolvedValues))
                .sorted(Comparator
                        .comparing(PipelineRunParameterResponse::getParamGroup,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(PipelineRunParameterResponse::getParamGroupSort,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    // ============================== 参数联动刷新 ==============================

    @Override
    public List<PipelineRunParameterResponse> refreshParameters(PipelineParametersRefreshRequest request) {
        if (request == null || request.getPipelineId() == null) {
            throw new BusinessException(PipelineConstants.MSG_PIPELINE_ID_REQUIRED);
        }
        if (!StringUtils.hasText(request.getChangedParamName())) {
            throw new BusinessException(PipelineParameterConstants.MSG_CHANGED_PARAM_NAME_REQUIRED);
        }
        Long pipelineId = request.getPipelineId();
        // 1. 查 pipeline + 模板参数名列表 + 参数定义（复用加载逻辑）
        Pipeline pipeline = pipelineRepository.selectById(pipelineId);
        if (pipeline == null) {
            throw new BusinessException(PipelineConstants.MSG_PIPELINE_NOT_EXIST);
        }
        PipelineTemplateVersion effective =
                pipelineTemplateVersionRepository.selectEffectiveByCode(pipeline.getPipelineTemplateCode());
        if (effective == null) {
            throw new BusinessException(String.format(
                    PipelineConstants.MSG_TEMPLATE_NO_EFFECTIVE_VERSION, pipeline.getPipelineTemplateCode()));
        }
        List<String> templateParamNames = ArgoWorkflowUtil.extractParamNames(
                effective.getTemplateDetail(), OBJECT_MAPPER);
        if (templateParamNames.isEmpty()) {
            return Collections.emptyList();
        }

        // 递归加载参数定义（模板参数 + 所有层级的依赖参数）
        List<PipelineParameter> allParams = loadParameterDefinitions(templateParamNames);
        Map<String, PipelineParameter> nameToParam = new LinkedHashMap<>();
        for (PipelineParameter p : allParams) {
            nameToParam.put(p.getName(), p);
        }

        // 2. 将 request.currentValues 合并到计算上下文
        Map<String, String> resolvedValues = new HashMap<>();
        if (request.getCurrentValues() != null) {
            resolvedValues.putAll(request.getCurrentValues());
        }
        ParamResolveContext context = ParamResolveContext.builder()
                .pipelineId(pipeline.getId())
                .appName(pipeline.getAppName())
                .pipelineTemplateCode(pipeline.getPipelineTemplateCode())
                .resolvedValues(resolvedValues)
                .build();

        // 3. 找到所有依赖 changedParamName 的参数（直接 + 间接，递归依赖链）
        String changedParamName = request.getChangedParamName();
        Set<String> affectedNames = findDownstreamDependents(changedParamName, nameToParam);

        // 4. 清除受影响参数的旧值（依赖参数变动后，下游参数需重置为默认值或清空）
        for (String affectedName : affectedNames) {
            resolvedValues.remove(affectedName);
        }

        // 5. 拓扑排序全部参数，逐个重新计算（已有值的跳过，被清除值的重新计算）
        List<PipelineParameter> sortedParams = topologicalSort(allParams);
        for (PipelineParameter param : sortedParams) {
            if (resolvedValues.containsKey(param.getName())) {
                continue;
            }
            String value = resolveParamValue(param, context);
            if (value != null) {
                resolvedValues.put(param.getName(), value);
            }
        }

        // 6. 返回全量 user 参数（含重新计算的值和过滤后的选项）
        return sortedParams.stream()
                .filter(p -> !ParamTypeEnum.SYSTEM.getCode().equals(p.getParamType()))
                .map(p -> toRunParameterResponse(p, resolvedValues))
                .sorted(Comparator
                        .comparing(PipelineRunParameterResponse::getParamGroup,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(PipelineRunParameterResponse::getParamGroupSort,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    /**
     * 递归加载参数定义：从模板参数出发，沿 dependParams 深度优先收集所有层级的依赖参数。
     * <p>使用 visited 集合防止循环依赖导致的无限递归。
     * <p>未定义的参数直接抛 BusinessException。
     *
     * @param templateParamNames 模板中声明的参数名列表
     * @return 参数定义列表（模板参数 + 所有层级的依赖参数）
     */
    private List<PipelineParameter> loadParameterDefinitions(List<String> templateParamNames) {
        Map<String, PipelineParameter> result = new LinkedHashMap<>();
        Set<String> visited = new HashSet<>();
        loadRecursively(templateParamNames, result, visited);
        return new ArrayList<>(result.values());
    }

    /**
     * 递归加载参数定义的核心实现。
     *
     * @param names    当前层级需要查询的参数名
     * @param result   已加载的参数定义（累积，key=参数名）
     * @param visited  已处理的参数名（防止循环依赖）
     */
    private void loadRecursively(List<String> names, Map<String, PipelineParameter> result, Set<String> visited) {
        // 过滤出尚未加载的参数名
        List<String> toQuery = names.stream()
                .filter(n -> !result.containsKey(n))
                .collect(Collectors.toList());
        if (toQuery.isEmpty()) {
            return;
        }
        // 批量查库
        List<PipelineParameter> defined = pipelineParameterRepository.listByNames(toQuery);
        // 检查未定义参数
        if (defined.size() < toQuery.size()) {
            Set<String> definedNames = defined.stream()
                    .map(PipelineParameter::getName)
                    .collect(Collectors.toSet());
            List<String> undefined = toQuery.stream()
                    .filter(n -> !definedNames.contains(n))
                    .collect(Collectors.toList());
            throw new BusinessException(String.format(
                    PipelineParameterConstants.MSG_PARAM_UNDEFINED, String.join(", ", undefined)));
        }
        for (PipelineParameter param : defined) {
            result.put(param.getName(), param);
            visited.add(param.getName());
        }
        // 收集下一层依赖
        List<String> nextLevel = new ArrayList<>();
        for (PipelineParameter param : defined) {
            for (String dep : parseDependParams(param.getDependParams())) {
                if (result.containsKey(dep) || visited.contains(dep)) {
                    continue; // 已加载或在当前路径上（环），跳过
                }
                nextLevel.add(dep);
            }
        }
        if (!nextLevel.isEmpty()) {
            loadRecursively(nextLevel, result, visited);
        }
    }

    /**
     * 递归查找所有直接或间接依赖 changedParamName 的参数名。
     * <p>例如 A 依赖 changedParamName，B 依赖 A，则 A 和 B 都会被找到。
     *
     * @param changedParamName 变动的参数名
     * @param nameToParam      所有参数定义（name → entity）
     * @return 受影响的参数名集合（不含 changedParamName 本身）
     */
    private Set<String> findDownstreamDependents(String changedParamName,
                                                 Map<String, PipelineParameter> nameToParam) {
        // 构建反向邻接表：被依赖参数 → 直接依赖它的参数列表
        Map<String, List<String>> reverseGraph = new HashMap<>();
        for (PipelineParameter param : nameToParam.values()) {
            List<String> depends = parseDependParams(param.getDependParams());
            for (String dep : depends) {
                reverseGraph.computeIfAbsent(dep, k -> new ArrayList<>()).add(param.getName());
            }
        }
        // BFS 从 changedParamName 出发，找到所有下游参数
        Set<String> result = new LinkedHashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.add(changedParamName);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            List<String> dependents = reverseGraph.get(current);
            if (dependents == null) {
                continue;
            }
            for (String dependent : dependents) {
                if (result.add(dependent)) {
                    queue.add(dependent);
                }
            }
        }
        return result;
    }

    /**
     * 拓扑排序：按 depend_params 对参数排序，被依赖的参数排在前面。
     * <p>使用 Kahn 算法（入度法），检测到环时跳过并记录告警日志。
     */
    private List<PipelineParameter> topologicalSort(List<PipelineParameter> params) {
        Map<String, PipelineParameter> nameToParam = new LinkedHashMap<>();
        for (PipelineParameter p : params) {
            nameToParam.put(p.getName(), p);
        }
        // 构建邻接表和入度
        Map<String, Set<String>> graph = new HashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();
        for (PipelineParameter p : params) {
            String name = p.getName();
            graph.putIfAbsent(name, new HashSet<>());
            inDegree.putIfAbsent(name, 0);
            List<String> depends = parseDependParams(p.getDependParams());
            for (String dep : depends) {
                // 只考虑在当前参数集合中的依赖
                if (nameToParam.containsKey(dep)) {
                    graph.computeIfAbsent(dep, k -> new HashSet<>()).add(name);
                    inDegree.merge(name, 1, Integer::sum);
                }
            }
        }
        // Kahn 算法
        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }
        List<PipelineParameter> sorted = new ArrayList<>(params.size());
        while (!queue.isEmpty()) {
            String current = queue.poll();
            sorted.add(nameToParam.get(current));
            for (String next : graph.getOrDefault(current, Collections.emptySet())) {
                int newDegree = inDegree.merge(next, -1, Integer::sum);
                if (newDegree == 0) {
                    queue.add(next);
                }
            }
        }
        // 环中的参数（入度未归零）追加到末尾
        if (sorted.size() < params.size()) {
            log.warn("参数依赖存在循环，环中参数将追加到末尾");
            for (PipelineParameter p : params) {
                if (!sorted.contains(p)) {
                    sorted.add(p);
                }
            }
        }
        return sorted;
    }

    /**
     * 解析 dependParams JSON 字符串为参数名列表。
     */
    private List<String> parseDependParams(String dependParamsJson) {
        if (!StringUtils.hasText(dependParamsJson)) {
            return Collections.emptyList();
        }
        try {
            return JSON.parseArray(dependParamsJson).toJavaList(String.class);
        } catch (Exception e) {
            log.warn("解析 dependParams 失败: {}", dependParamsJson, e);
            return Collections.emptyList();
        }
    }

    /**
     * 计算单个参数的初始值。
     * <p>统一通过 {@link PipelineParameterStrategyManager} 按参数名路由到对应策略：
     * <ul>
     *     <li>有对应策略类（如 app-name、git-url）→ 执行该策略的自定义逻辑；</li>
     *     <li>无对应策略类 → 走 {@code DefaultPipelineParameterStrategy}，按 priority 降序遍历
     *         默认值策略链，取第一个非 null；全 null 用 defaultValue 兜底。</li>
     * </ul>
     */
    private String resolveParamValue(PipelineParameter param, ParamResolveContext context) {
        return pipelineParameterStrategyManager.getStrategy(param.getName()).buildParameter(param, context);
    }

    /**
     * 将参数定义实体转换为执行弹框响应，填充已计算的值和选项。
     */
    private PipelineRunParameterResponse toRunParameterResponse(PipelineParameter param,
                                                                Map<String, String> resolvedValues) {
        PipelineRunParameterResponse response = new PipelineRunParameterResponse();
        response.setName(param.getName());
        response.setLabel(param.getLabel());
        response.setDescription(param.getDescription());
        response.setComponentType(param.getComponentType() != null ? param.getComponentType()
                : ComponentTypeEnum.INPUT.getCode());
        response.setParamType(param.getParamType());
        response.setRequired(param.getRequired());
        response.setRefreshOnChanged(param.getRefreshOnChanged());
        response.setRegexPattern(param.getRegexPattern());
        response.setParamGroup(param.getParamGroup());
        response.setParamGroupSort(param.getParamGroupSort());
        // 解析选项配置并按 parameterDepends + resolvedValues 做服务端过滤（select / radio 用）
        List<PipelineRunParameterResponse.OptionItem> allOptions = parseOptionItems(param.getOptionConfig());
        response.setOptions(filterOptions(allOptions, resolvedValues));
        // 计算初始值：优先策略/默认值，若为空则从选项中取 asDefault 的选项值
        String resolvedValue = resolvedValues.get(param.getName());
        if (resolvedValue == null || resolvedValue.isEmpty()) {
            resolvedValue = allOptions.stream()
                    .filter(o -> Boolean.TRUE.equals(o.getAsDefault()))
                    .map(PipelineRunParameterResponse.OptionItem::getValue)
                    .findFirst()
                    .orElse(null);
        }
        response.setValue(resolvedValue);
        response.setHidden(false);
        return response;
    }

    /**
     * 按当前已解析的参数值过滤选项。
     * <p>选项的 parameterDepends 为 null/空时无条件显示；
     * 否则需所有条件匹配（依赖参数当前值 === 条件值）才显示。
     *
     * @param options        全部选项
     * @param resolvedValues 当前已解析的参数值
     * @return 过滤后的可见选项
     */
    private List<PipelineRunParameterResponse.OptionItem> filterOptions(
            List<PipelineRunParameterResponse.OptionItem> options, Map<String, String> resolvedValues) {
        if (options == null || options.isEmpty()) {
            return Collections.emptyList();
        }
        List<PipelineRunParameterResponse.OptionItem> visible = new ArrayList<>(options.size());
        for (PipelineRunParameterResponse.OptionItem opt : options) {
            if (opt.getParameterDepends() == null || opt.getParameterDepends().isEmpty()) {
                visible.add(opt);
                continue;
            }
            boolean allMatch = true;
            for (PipelineRunParameterResponse.OptionDepend dep : opt.getParameterDepends()) {
                String currentVal = resolvedValues.get(dep.getName());
                if (currentVal == null || !currentVal.equals(dep.getValue())) {
                    allMatch = false;
                    break;
                }
            }
            if (allMatch) {
                visible.add(opt);
            }
        }
        return visible;
    }

    /**
     * 解析 optionConfig JSON 为选项列表。
     */
    private List<PipelineRunParameterResponse.OptionItem> parseOptionItems(String optionConfigJson) {
        if (!StringUtils.hasText(optionConfigJson)) {
            return Collections.emptyList();
        }
        try {
            JSONArray arr = JSON.parseArray(optionConfigJson);
            List<PipelineRunParameterResponse.OptionItem> items = new ArrayList<>(arr.size());
            for (int i = 0; i < arr.size(); i++) {
                com.alibaba.fastjson.JSONObject obj = arr.getJSONObject(i);
                PipelineRunParameterResponse.OptionItem item = new PipelineRunParameterResponse.OptionItem();
                item.setValue(obj.getString("value"));
                item.setLabel(obj.getString("label") != null ? obj.getString("label") : obj.getString("value"));
                item.setAsDefault(obj.getBoolean("asDefault"));
                // 解析 parameterDepends（选项显示条件）
                com.alibaba.fastjson.JSONArray dependsArr = obj.getJSONArray("parameterDepends");
                if (dependsArr != null && !dependsArr.isEmpty()) {
                    List<PipelineRunParameterResponse.OptionDepend> depends = new ArrayList<>(dependsArr.size());
                    for (int j = 0; j < dependsArr.size(); j++) {
                        com.alibaba.fastjson.JSONObject dep = dependsArr.getJSONObject(j);
                        PipelineRunParameterResponse.OptionDepend od = new PipelineRunParameterResponse.OptionDepend();
                        od.setName(dep.getString("name"));
                        od.setValue(dep.getString("value"));
                        depends.add(od);
                    }
                    item.setParameterDepends(depends);
                }
                items.add(item);
            }
            return items;
        } catch (Exception e) {
            log.warn("解析 optionConfig 失败: {}", optionConfigJson, e);
            return Collections.emptyList();
        }
    }

    // ============================== 校验逻辑 ==============================

    private void validateRequiredFields(String name, String label, String paramType, String paramGroup) {
        if (!StringUtils.hasText(name)) {
            throw new BusinessException(PipelineParameterConstants.MSG_NAME_REQUIRED);
        }
        if (!StringUtils.hasText(label)) {
            throw new BusinessException(PipelineParameterConstants.MSG_LABEL_REQUIRED);
        }
        if (!StringUtils.hasText(paramType)) {
            throw new BusinessException(PipelineParameterConstants.MSG_PARAM_TYPE_REQUIRED);
        }
        if (!StringUtils.hasText(paramGroup)) {
            throw new BusinessException(PipelineParameterConstants.MSG_PARAM_GROUP_REQUIRED);
        }
    }

    private void validateNameFormat(String name) {
        if (!NAME_PATTERN.matcher(name).matches()) {
            throw new BusinessException(PipelineParameterConstants.MSG_NAME_FORMAT);
        }
    }

    private void validateEnumFields(String componentType, String paramType) {
        if (!ParamTypeEnum.isValidCode(paramType)) {
            throw new BusinessException(String.format(
                    PipelineParameterConstants.MSG_PARAM_TYPE_INVALID, paramType));
        }
        if (StringUtils.hasText(componentType) && !ComponentTypeEnum.isValidCode(componentType)) {
            throw new BusinessException(String.format(
                    PipelineParameterConstants.MSG_COMPONENT_TYPE_INVALID, componentType));
        }
    }

    /**
     * 校验 JSON 格式字段：optionConfig、defaultValueStrategyConfig、dependParams。
     */
    private void validateJsonFields(String optionConfig, String strategyConfig, String dependParams) {
        if (StringUtils.hasText(optionConfig)) {
            try {
                JSONArray arr = JSON.parseArray(optionConfig);
                for (int i = 0; i < arr.size(); i++) {
                    arr.getJSONObject(i);
                }
            } catch (Exception e) {
                throw new BusinessException(PipelineParameterConstants.MSG_OPTION_CONFIG_INVALID);
            }
        }
        if (StringUtils.hasText(strategyConfig)) {
            try {
                JSONArray arr = JSON.parseArray(strategyConfig);
                for (int i = 0; i < arr.size(); i++) {
                    String strategyType = arr.getJSONObject(i).getString("strategyType");
                    if (!DefaultValueStrategyTypeEnum.isValidCode(strategyType)) {
                        throw new BusinessException(PipelineParameterConstants.MSG_STRATEGY_CONFIG_INVALID);
                    }
                }
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                throw new BusinessException(PipelineParameterConstants.MSG_STRATEGY_CONFIG_INVALID);
            }
        }
        if (StringUtils.hasText(dependParams)) {
            try {
                JSON.parseArray(dependParams).toJavaList(String.class);
            } catch (Exception e) {
                throw new BusinessException(PipelineParameterConstants.MSG_DEPEND_PARAMS_INVALID);
            }
        }
    }

    /**
     * 校验依赖参数：引用的参数名必须已存在（排除自身），且不存在循环依赖。
     *
     * @param dependParamsJson 依赖参数 JSON 数组字符串
     * @param selfId           当前参数 id（更新时排除自身，新增时为 null）
     * @param selfName         当前参数名（新增时从前端传入，更新时从库查）
     */
    private void validateDependParams(String dependParamsJson, Long selfId, String selfName) {
        if (!StringUtils.hasText(dependParamsJson)) {
            return;
        }
        List<String> dependNames = JSON.parseArray(dependParamsJson).toJavaList(String.class);

        // 1. 校验被依赖的参数是否存在
        for (String dependName : dependNames) {
            PipelineParameter dependParam = pipelineParameterRepository.selectByName(dependName);
            if (dependParam == null || (selfId != null && dependParam.getId().equals(selfId))) {
                throw new BusinessException(String.format(
                        PipelineParameterConstants.MSG_DEPEND_PARAM_NOT_EXIST, dependName));
            }
        }

        // 2. 循环依赖检测：从当前参数的依赖出发，深度优先遍历，如果能回到当前参数则存在环
        checkCircularDependency(dependNames, selfName);
    }

    /**
     * 循环依赖检测：从当前参数的直接依赖出发，DFS 遍历，如果能回到当前参数名则存在环。
     * <p>每个分支使用独立的 visited 集合，避免跨分支共享导致漏检。
     */
    private void checkCircularDependency(List<String> directDependNames, String selfName) {
        for (String dependName : directDependNames) {
            if (hasCycle(dependName, selfName, new HashSet<>())) {
                throw new BusinessException(String.format(
                        PipelineParameterConstants.MSG_DEPEND_CIRCULAR,
                        selfName != null ? selfName : "new-param"));
            }
        }
    }

    /**
     * DFS 检测：从 currentName 出发，是否能到达 targetName。
     *
     * @param currentName 当前遍历到的参数名
     * @param targetName  目标参数名（当前正在保存的参数），到达则说明有环
     * @param visited     当前分支已访问的参数名集合（每个分支独立）
     */
    private boolean hasCycle(String currentName, String targetName, Set<String> visited) {
        if (currentName.equals(targetName)) {
            return true;
        }
        if (visited.contains(currentName)) {
            return false;
        }
        visited.add(currentName);

        PipelineParameter param = pipelineParameterRepository.selectByName(currentName);
        if (param == null || !StringUtils.hasText(param.getDependParams())) {
            return false;
        }
        List<String> subDepends = JSON.parseArray(param.getDependParams()).toJavaList(String.class);
        for (String subDepend : subDepends) {
            if (hasCycle(subDepend, targetName, visited)) {
                return true;
            }
        }
        return false;
    }

    // ============================== 工具方法 ==============================

    /**
     * 为 Boolean / Integer 字段设置默认值（当前端未传时）。
     */
    private void applyDefaults(PipelineParameter entity) {
        if (entity.getRequired() == null) {
            entity.setRequired(false);
        }
        if (entity.getNeedSystemProcess() == null) {
            entity.setNeedSystemProcess(false);
        }
        if (entity.getRefreshOnChanged() == null) {
            entity.setRefreshOnChanged(false);
        }
        if (entity.getParamGroupSort() == null) {
            entity.setParamGroupSort(0);
        }
        if (entity.getDeleted() == null) {
            entity.setDeleted(0);
        }
    }

    private PipelineParameterResponse toResponse(PipelineParameter entity) {
        if (entity == null) {
            return null;
        }
        PipelineParameterResponse response = new PipelineParameterResponse();
        BeanUtils.copyProperties(entity, response);
        return response;
    }
}
