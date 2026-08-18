package com.ci.pipeline.service.concurrency;

import com.ci.pipeline.common.constants.PipelineConcurrencyConstants;
import com.ci.pipeline.common.enums.PipelineRunStatusEnum;
import com.ci.pipeline.common.exception.BusinessException;
import com.ci.pipeline.dao.entity.GenericConfig;
import com.ci.pipeline.dao.entity.Pipeline;
import com.ci.pipeline.dao.entity.PipelineRun;
import com.ci.pipeline.dao.entity.PipelineTemplate;
import com.ci.pipeline.dao.repository.GenericConfigRepository;
import com.ci.pipeline.dao.repository.PipelineRunRepository;
import com.ci.pipeline.service.config.PipelineConcurrencyProperties;
import com.ci.pipeline.service.concurrency.policy.OverLimitPolicyStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * 流水线执行前并发检查编排：L1 全局限流 → L2 应用×模板配额 → L3 流水线配额。
 * <p>额度统计口径：Pending / Running / Unknown 状态的执行记录占用额度
 * （Failed / Error 不占额度，失败后可立即重试；Unknown 是短暂态需计入防止漏算）。
 * <p>超限处理动作委托 {@link OverLimitPolicyStrategyManager} 按策略编码路由到
 * {@link com.ci.pipeline.service.concurrency.policy.OverLimitPolicyStrategy} 实现
 * （Reject / ReplaceOldest），新增策略零改动本类（开闭原则）。
 * <p>ReplaceOldest 的替换范围仅限当前流水线自己的最早一条执行；
 * 整个检查过程最多终止一条执行（该记录同时计入 L2 与 L3 额度，终止它即同时满足两层）。
 * <p>check-then-act 不加锁，接受多实例并发提交时的少量超卖（CI 场景额度是软保护）。
 */
@Slf4j
@Component
public class PipelineConcurrencyChecker {

    /** 占用额度的状态集合：Pending / Running / Unknown */
    private static final List<String> OCCUPYING_STATUSES = Arrays.asList(
            PipelineRunStatusEnum.PENDING.getCode(),
            PipelineRunStatusEnum.RUNNING.getCode(),
            PipelineRunStatusEnum.UNKNOWN.getCode());

    @Autowired
    private GenericConfigRepository genericConfigRepository;

    @Autowired
    private PipelineRunRepository pipelineRunRepository;

    @Autowired
    private OverLimitPolicyStrategyManager overLimitPolicyStrategyManager;

    @Autowired
    private PipelineConcurrencyProperties properties;

    /**
     * 执行前并发检查：L1 全局 → L2 应用×模板 → L3 流水线。
     * 任一层超限且策略判定阻断时抛 BusinessException；
     * ReplaceOldest 则终止本流水线最早一条占用记录后放行（全程最多终止一条）。
     */
    public void checkBeforeExecute(Pipeline pipeline, PipelineTemplate template) {
        checkGlobalLimit();                                  // L1
        boolean replaced = checkAppTemplateLimit(pipeline, template);  // L2
        if (!replaced) {
            checkPipelineLimit(pipeline, template);          // L3（L2 已替换时 L3 必然满足，跳过）
        }
    }

    /**
     * 计算流水线维度的生效并发上限：
     * pipeline.max_running_limit 未配置时 fallback 到模板值；
     * 配置值 clamp 到模板值（模板值既是默认值也是上限）；模板值异常时兜底常量 1。
     */
    public int resolveEffectiveLimit(Pipeline pipeline, PipelineTemplate template) {
        int templateLimit = template != null && template.getAppMaxRunningLimit() != null
                ? template.getAppMaxRunningLimit()
                : PipelineConcurrencyConstants.DEFAULT_MAX_RUNNING_LIMIT;
        if (templateLimit < 1) {
            templateLimit = PipelineConcurrencyConstants.DEFAULT_MAX_RUNNING_LIMIT;
        }
        Integer pipelineLimit = pipeline != null ? pipeline.getMaxRunningLimit() : null;
        if (pipelineLimit == null) {
            return templateLimit;
        }
        return Math.min(pipelineLimit, templateLimit);
    }

    /**
     * 解析生效的超限策略编码：pipeline 优先，NULL 取模板，均未配置兜底 Reject
     */
    public String resolvePolicyCode(Pipeline pipeline, PipelineTemplate template) {
        String code = pipeline != null ? pipeline.getOverLimitPolicy() : null;
        if (!StringUtils.hasText(code) && template != null) {
            code = template.getOverLimitPolicy();
        }
        return StringUtils.hasText(code) ? code : PipelineConcurrencyConstants.DEFAULT_OVER_LIMIT_POLICY;
    }

    // ===== L1 全局限流（只做 Reject，容量保护伞不做替换） =====

    private void checkGlobalLimit() {
        long occupying = pipelineRunRepository.countOccupying(OCCUPYING_STATUSES);
        int globalLimit = getGlobalLimit();
        if (occupying >= globalLimit) {
            log.warn("全局限流拦截: 占用={} 上限={}", occupying, globalLimit);
            throw new BusinessException(String.format(
                    PipelineConcurrencyConstants.MSG_GLOBAL_LIMIT_EXCEEDED, globalLimit));
        }
    }

    // ===== L2 应用×模板配额 =====

    /**
     * @return ReplaceOldest 已执行替换时返回 true（调用方据此跳过 L3）
     */
    private boolean checkAppTemplateLimit(Pipeline pipeline, PipelineTemplate template) {
        int appLimit = template != null && template.getAppMaxRunningLimit() != null
                ? template.getAppMaxRunningLimit()
                : PipelineConcurrencyConstants.DEFAULT_MAX_RUNNING_LIMIT;
        if (appLimit < 1) {
            appLimit = PipelineConcurrencyConstants.DEFAULT_MAX_RUNNING_LIMIT;
        }
        long occupying = pipelineRunRepository.countOccupyingByAppAndTemplate(
                pipeline.getAppName(), pipeline.getPipelineTemplateCode(), OCCUPYING_STATUSES);
        if (occupying < appLimit) {
            return false;
        }
        return handleOverLimit(pipeline, template, appLimit, occupying,
                PipelineConcurrencyConstants.MSG_APP_TEMPLATE_LIMIT_EXCEEDED,
                pipeline.getAppName(), pipeline.getPipelineTemplateCode(), appLimit);
    }

    // ===== L3 流水线配额 =====

    private void checkPipelineLimit(Pipeline pipeline, PipelineTemplate template) {
        int effectiveLimit = resolveEffectiveLimit(pipeline, template);
        long occupying = pipelineRunRepository.countOccupyingByPipelineId(pipeline.getId(), OCCUPYING_STATUSES);
        if (occupying < effectiveLimit) {
            return;
        }
        handleOverLimit(pipeline, template, effectiveLimit, occupying,
                PipelineConcurrencyConstants.MSG_PIPELINE_LIMIT_EXCEEDED,
                pipeline.getName(), effectiveLimit);
    }

    /**
     * 超限统一处理：委托策略实现判断阻断 / 腾位。
     *
     * @return 策略已执行替换（腾位）时返回 true（调用方据此跳过下一层检查）
     */
    private boolean handleOverLimit(Pipeline pipeline, PipelineTemplate template, int limit,
                                    long occupying, String rejectMessage, Object... messageArgs) {
        String policyCode = resolvePolicyCode(pipeline, template);
        OverLimitPolicyStrategy strategy = overLimitPolicyStrategyManager.getStrategy(policyCode);
        // 本流水线自身的占用数与占用列表（ReplaceOldest 判断能否腾位 / 选择终止目标用）
        long ownOccupying = pipelineRunRepository.countOccupyingByPipelineId(
                pipeline.getId(), OCCUPYING_STATUSES);
        if (strategy.isBlock(pipeline, template, limit, occupying, ownOccupying)) {
            log.warn("并发配额拦截: pipelineId={}, 层级上限={}, 占用={}, 策略={}",
                    pipeline.getId(), limit, occupying, policyCode);
            throw new BusinessException(String.format(rejectMessage, messageArgs));
        }
        List<PipelineRun> ownRuns = pipelineRunRepository.selectOccupyingByPipelineId(
                pipeline.getId(), OCCUPYING_STATUSES);
        return strategy.beforeExecute(pipeline, ownRuns);
    }

    /**
     * 全局最大运行数：generic_config 表优先，未配置或非法时兜底 yml 默认值
     */
    private int getGlobalLimit() {
        GenericConfig config = genericConfigRepository.getByKey(
                PipelineConcurrencyConstants.CONFIG_KEY_MAX_RUNNING_LIMIT);
        if (config != null && StringUtils.hasText(config.getConfigValue())) {
            try {
                return Integer.parseInt(config.getConfigValue().trim());
            } catch (NumberFormatException e) {
                log.warn("全局并发上限配置非法: value={}, 使用 yml 兜底值", config.getConfigValue());
            }
        }
        return properties.getMaxRunningLimit();
    }
}

