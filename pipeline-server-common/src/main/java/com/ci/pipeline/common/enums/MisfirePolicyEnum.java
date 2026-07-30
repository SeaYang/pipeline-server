package com.ci.pipeline.common.enums;

/**
 * 定时任务错过执行（misfire）处理策略枚举。
 * <p>对应 cron_job.misfire_policy 字段。
 * <ul>
 *     <li>{@link #FIRE_NOW} — 立即补偿执行一次；</li>
 *     <li>{@link #FIRE_ONCE} — 仅补偿一次（当前实现与 FIRE_NOW 行为一致，预留区分未来"合并多次错过"场景）；</li>
 *     <li>{@link #SKIP} — 直接跳过，等待下一次正常触发时间。</li>
 * </ul>
 */
public enum MisfirePolicyEnum {

    FIRE_NOW("fire_now", "错过后立即执行一次"),
    FIRE_ONCE("fire_once", "错过后仅补偿执行一次"),
    SKIP("skip", "错过后直接跳过");

    private final String code;
    private final String description;

    MisfirePolicyEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static MisfirePolicyEnum ofCode(String code) {
        if (code == null) {
            return null;
        }
        for (MisfirePolicyEnum policy : values()) {
            if (policy.code.equals(code)) {
                return policy;
            }
        }
        return null;
    }

    public static boolean isValidCode(String code) {
        return ofCode(code) != null;
    }
}
