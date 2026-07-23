package com.ci.pipeline.service.strategy.defaultvalue;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ci.pipeline.service.strategy.ParamResolveContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 默认值策略链管理器。
 * <p>启动时自动收集所有 {@link DefaultValueStrategy} Bean，按 {@link DefaultValueStrategy#strategyType()} 建立路由表。
 * 调用 {@link #resolve} 时解析策略配置 JSON，按 priority 降序遍历策略，取第一个非 null 结果。
 */
@Slf4j
@Component
public class DefaultValueStrategyManager {

    private final Map<String, DefaultValueStrategy> strategies;

    @Autowired
    public DefaultValueStrategyManager(List<DefaultValueStrategy> strategyList) {
        if (strategyList == null || strategyList.isEmpty()) {
            this.strategies = Collections.emptyMap();
        } else {
            this.strategies = strategyList.stream()
                    .collect(Collectors.toMap(DefaultValueStrategy::strategyType, s -> s));
        }
        log.info("DefaultValueStrategyManager 初始化完成, 已注册策略: {}", strategies.keySet());
    }

    /**
     * 按 priority 降序遍历策略，取第一个非 null 结果。
     *
     * @param paramName           参数名
     * @param strategyConfigJson  默认值策略配置 JSON（default_value_strategy_config 字段）
     * @param context             参数计算上下文
     * @return 默认值，全部未命中返回 null（由调用方用 defaultValue 兜底）
     */
    public String resolve(String paramName, String strategyConfigJson, ParamResolveContext context) {
        if (strategyConfigJson == null || strategyConfigJson.trim().isEmpty()) {
            return null;
        }
        List<StrategyConfig> configs = parseConfig(strategyConfigJson);
        if (configs.isEmpty()) {
            return null;
        }
        // priority 降序
        configs.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
        for (StrategyConfig config : configs) {
            DefaultValueStrategy strategy = strategies.get(config.getStrategyType());
            if (strategy == null) {
                log.debug("默认值策略未注册, strategyType={}, paramName={}", config.getStrategyType(), paramName);
                continue;
            }
            String value = strategy.getValue(paramName, context);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    /**
     * 解析策略配置 JSON。
     * <p>格式：[{"strategyType":"LastSuccessfulRun","priority":1},{"strategyType":"AppConfig","priority":0}]
     */
    private List<StrategyConfig> parseConfig(String json) {
        try {
            JSONArray arr = JSON.parseArray(json);
            if (arr == null || arr.isEmpty()) {
                return Collections.emptyList();
            }
            return arr.stream()
                    .map(item -> {
                        JSONObject obj = (JSONObject) item;
                        StrategyConfig config = new StrategyConfig();
                        config.setStrategyType(obj.getString("strategyType"));
                        config.setPriority(obj.getIntValue("priority"));
                        return config;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("解析默认值策略配置失败: {}", json, e);
            return Collections.emptyList();
        }
    }

    /**
     * 策略配置项。
     */
    @lombok.Data
    public static class StrategyConfig {
        private String strategyType;
        private int priority;
    }
}
