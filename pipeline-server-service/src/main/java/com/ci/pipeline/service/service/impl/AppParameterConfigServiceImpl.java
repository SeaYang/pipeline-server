package com.ci.pipeline.service.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ci.pipeline.common.constants.AppParameterConfigConstants;
import com.ci.pipeline.dao.entity.AppParameterConfig;
import com.ci.pipeline.dao.entity.PipelineParameter;
import com.ci.pipeline.dao.repository.AppParameterConfigRepository;
import com.ci.pipeline.dao.repository.PipelineParameterRepository;
import com.ci.pipeline.facade.request.AppParameterConfigBatchCreateRequest;
import com.ci.pipeline.facade.request.AppParameterConfigCreateRequest;
import com.ci.pipeline.facade.request.AppParameterConfigQueryRequest;
import com.ci.pipeline.facade.request.AppParameterConfigUpdateRequest;
import com.ci.pipeline.facade.response.AppParameterConfigResponse;
import com.ci.pipeline.common.exception.BusinessException;
import com.ci.pipeline.service.service.AppParameterConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AppParameterConfigServiceImpl implements AppParameterConfigService {

    @Autowired
    private AppParameterConfigRepository appParameterConfigRepository;

    @Autowired
    private PipelineParameterRepository pipelineParameterRepository;

    @Override
    public AppParameterConfigResponse create(AppParameterConfigCreateRequest request) {
        validateCreateRequired(request);
        checkUnique(request.getAppName(), request.getParameterName(), request.getEnv(), null);

        AppParameterConfig entity = new AppParameterConfig();
        entity.setAppName(request.getAppName());
        entity.setParameterName(request.getParameterName());
        entity.setValue(request.getValue());
        entity.setEnv(request.getEnv());
        appParameterConfigRepository.insert(entity);
        log.info("新增应用参数配置成功, appName={}, parameterName={}, env={}",
                entity.getAppName(), entity.getParameterName(), entity.getEnv());
        return toResponse(appParameterConfigRepository.selectById(entity.getId()), null);
    }

    @Override
    public void batchCreate(AppParameterConfigBatchCreateRequest request) {
        if (!StringUtils.hasText(request.getAppName())) {
            throw new BusinessException(AppParameterConfigConstants.MSG_APP_NAME_REQUIRED);
        }
        if (!StringUtils.hasText(request.getEnv())) {
            throw new BusinessException(AppParameterConfigConstants.MSG_ENV_REQUIRED);
        }
        List<AppParameterConfigBatchCreateRequest.ConfigItem> items = request.getItems();
        if (items == null || items.isEmpty()) {
            throw new BusinessException(AppParameterConfigConstants.MSG_BATCH_ITEMS_EMPTY);
        }

        // 校验批次内无重复参数名
        Set<String> seen = new HashSet<>();
        for (AppParameterConfigBatchCreateRequest.ConfigItem item : items) {
            if (!StringUtils.hasText(item.getParameterName())) {
                throw new BusinessException(AppParameterConfigConstants.MSG_PARAMETER_NAME_REQUIRED);
            }
            if (!StringUtils.hasText(item.getValue())) {
                throw new BusinessException(AppParameterConfigConstants.MSG_VALUE_REQUIRED);
            }
            if (!seen.add(item.getParameterName())) {
                throw new BusinessException(String.format(
                        AppParameterConfigConstants.MSG_BATCH_DUPLICATE_PARAM, item.getParameterName()));
            }
        }

        // 逐条校验唯一性
        for (AppParameterConfigBatchCreateRequest.ConfigItem item : items) {
            checkUnique(request.getAppName(), item.getParameterName(), request.getEnv(), null);
        }

        // 逐条插入
        for (AppParameterConfigBatchCreateRequest.ConfigItem item : items) {
            AppParameterConfig entity = new AppParameterConfig();
            entity.setAppName(request.getAppName());
            entity.setParameterName(item.getParameterName());
            entity.setValue(item.getValue());
            entity.setEnv(request.getEnv());
            appParameterConfigRepository.insert(entity);
        }
        log.info("批量新增应用参数配置成功, appName={}, env={}, count={}",
                request.getAppName(), request.getEnv(), items.size());
    }

    @Override
    public AppParameterConfigResponse update(AppParameterConfigUpdateRequest request) {
        if (request.getId() == null) {
            throw new BusinessException(AppParameterConfigConstants.MSG_ID_REQUIRED);
        }
        if (!StringUtils.hasText(request.getValue())) {
            throw new BusinessException(AppParameterConfigConstants.MSG_VALUE_REQUIRED);
        }
        AppParameterConfig existing = appParameterConfigRepository.selectById(request.getId());
        if (existing == null) {
            throw new BusinessException(AppParameterConfigConstants.MSG_NOT_EXIST);
        }
        AppParameterConfig entity = new AppParameterConfig();
        entity.setId(request.getId());
        entity.setValue(request.getValue());
        appParameterConfigRepository.updateById(entity);
        log.info("修改应用参数配置成功, id={}", request.getId());
        return toResponse(appParameterConfigRepository.selectById(request.getId()), null);
    }

    @Override
    public void deleteById(Long id) {
        AppParameterConfig existing = appParameterConfigRepository.selectById(id);
        if (existing == null) {
            throw new BusinessException(AppParameterConfigConstants.MSG_NOT_EXIST);
        }
        appParameterConfigRepository.deleteById(id);
        log.info("删除应用参数配置成功, id={}, appName={}, parameterName={}, env={}",
                id, existing.getAppName(), existing.getParameterName(), existing.getEnv());
    }

    @Override
    public List<AppParameterConfigResponse> list(AppParameterConfigQueryRequest query) {
        if (!StringUtils.hasText(query.getAppName())) {
            throw new BusinessException(AppParameterConfigConstants.MSG_APP_NAME_REQUIRED);
        }
        if (!StringUtils.hasText(query.getEnv())) {
            throw new BusinessException(AppParameterConfigConstants.MSG_ENV_REQUIRED);
        }
        List<AppParameterConfig> configs = appParameterConfigRepository.selectListByAppEnv(
                query.getAppName(), query.getEnv());
        if (configs == null || configs.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量查询 pipeline_parameter 的 label 和 description
        Set<String> paramNames = configs.stream()
                .map(AppParameterConfig::getParameterName)
                .collect(Collectors.toSet());
        Map<String, PipelineParameter> paramMap = pipelineParameterRepository.listByNames(paramNames)
                .stream()
                .collect(Collectors.toMap(PipelineParameter::getName, p -> p, (a, b) -> a, LinkedHashMap::new));

        return configs.stream()
                .map(c -> toResponse(c, paramMap.get(c.getParameterName())))
                .collect(Collectors.toList());
    }

    @Override
    public List<String> listEnvs() {
        List<String> envs = new ArrayList<>();
        // 第一个位置固定为 default
        envs.add(AppParameterConfigConstants.DEFAULT_ENV);

        // 从 pipeline_parameter 表查询 name=env 的记录，解析 option_config
        PipelineParameter envParam = pipelineParameterRepository.selectByName(
                AppParameterConfigConstants.PARAM_NAME_ENV);
        if (envParam != null && StringUtils.hasText(envParam.getOptionConfig())) {
            JSONArray arr = JSON.parseArray(envParam.getOptionConfig());
            for (int i = 0; i < arr.size(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String value = obj.getString("value");
                if (StringUtils.hasText(value) && !envs.contains(value)) {
                    envs.add(value);
                }
            }
        }
        return envs;
    }

    @Override
    public String getValue(String appName, String parameterName, String env) {
        // 1. 先查指定 env
        if (StringUtils.hasText(env)) {
            AppParameterConfig config = appParameterConfigRepository.selectByAppParamEnv(
                    appName, parameterName, env);
            if (config != null) {
                return config.getValue();
            }
        }
        // 2. 兜底查 default env
        AppParameterConfig defaultConfig = appParameterConfigRepository.selectByAppParamEnv(
                appName, parameterName, AppParameterConfigConstants.DEFAULT_ENV);
        return defaultConfig != null ? defaultConfig.getValue() : null;
    }

    // ===== 私有方法 =====

    private void validateCreateRequired(AppParameterConfigCreateRequest request) {
        if (!StringUtils.hasText(request.getAppName())) {
            throw new BusinessException(AppParameterConfigConstants.MSG_APP_NAME_REQUIRED);
        }
        if (!StringUtils.hasText(request.getParameterName())) {
            throw new BusinessException(AppParameterConfigConstants.MSG_PARAMETER_NAME_REQUIRED);
        }
        if (!StringUtils.hasText(request.getValue())) {
            throw new BusinessException(AppParameterConfigConstants.MSG_VALUE_REQUIRED);
        }
        if (!StringUtils.hasText(request.getEnv())) {
            throw new BusinessException(AppParameterConfigConstants.MSG_ENV_REQUIRED);
        }
    }

    private void checkUnique(String appName, String parameterName, String env, Long excludeId) {
        long count = appParameterConfigRepository.countByAppParamEnv(appName, parameterName, env, excludeId);
        if (count > 0) {
            throw new BusinessException(String.format(
                    AppParameterConfigConstants.MSG_DUPLICATED, appName, parameterName, env));
        }
    }

    private AppParameterConfigResponse toResponse(AppParameterConfig entity, PipelineParameter param) {
        if (entity == null) {
            return null;
        }
        AppParameterConfigResponse response = new AppParameterConfigResponse();
        response.setId(entity.getId());
        response.setAppName(entity.getAppName());
        response.setParameterName(entity.getParameterName());
        response.setValue(entity.getValue());
        response.setEnv(entity.getEnv());
        response.setCreateTime(entity.getCreateTime());
        response.setUpdateTime(entity.getUpdateTime());
        if (param != null) {
            response.setLabel(param.getLabel());
            response.setDescription(param.getDescription());
        }
        return response;
    }
}
