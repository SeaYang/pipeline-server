package com.ci.pipeline.common.util;

import org.springframework.scheduling.support.CronExpression;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * CRON 表达式工具类。
 * <p>使用 Spring 提供的 6 位（秒 分 时 日 月 周）CRON 语法：{@link CronExpression}。
 */
public final class CronUtils {

    private CronUtils() {
    }

    /**
     * 校验 CRON 表达式是否合法。
     */
    public static boolean isValid(String cronExpr) {
        if (cronExpr == null || cronExpr.trim().isEmpty()) {
            return false;
        }
        try {
            CronExpression.parse(cronExpr);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 计算 CRON 表达式从当前时刻起的下一次触发时间。
     *
     * @param cronExpr CRON 表达式
     * @return 下一次触发时间；表达式非法或不存在下一次触发时间时返回 null
     */
    public static Date getNextExecution(String cronExpr) {
        try {
            CronExpression expression = CronExpression.parse(cronExpr);
            LocalDateTime next = expression.next(LocalDateTime.now());
            if (next == null) {
                return null;
            }
            return Date.from(next.atZone(ZoneId.systemDefault()).toInstant());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
