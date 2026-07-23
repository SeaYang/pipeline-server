package com.ci.pipeline.common.util;

import com.ci.pipeline.common.constants.DictConstants;
import com.ci.pipeline.common.exception.BusinessException;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * 分页排序参数解析工具。
 * <p>
 * 由于 ORDER BY 的列名只能通过 SQL 字符串拼接（不能参数绑定），排序字段必须在调用方维护
 * 白名单后交由本工具统一解析，避免 SQL 注入。
 */
public final class SortUtil {

    private SortUtil() {
    }

    /**
     * 解析排序方向。
     * <ul>
     *     <li>空白 → 默认 {@code desc}；</li>
     *     <li>非 asc / desc（大小写不敏感）→ 抛 {@link BusinessException}。</li>
     * </ul>
     *
     * @param order 前端传入的排序方向，可为空
     * @return 归一化后的 asc / desc
     */
    public static String resolveOrder(String order) {
        if (!StringUtils.hasText(order)) {
            return DictConstants.SORT_ORDER_DESC;
        }
        String normalized = order.trim().toLowerCase();
        if (!DictConstants.SORT_ORDER_ASC.equals(normalized)
                && !DictConstants.SORT_ORDER_DESC.equals(normalized)) {
            throw new BusinessException(DictConstants.MSG_SORT_ORDER_INVALID);
        }
        return normalized;
    }

    /**
     * 解析排序字段：将出参字段名（camelCase）经白名单映射为数据库列名（snake_case）。
     * <ul>
     *     <li>空白 → 返回 {@code null}，由调用方走默认排序；</li>
     *     <li>不在白名单内 → 抛 {@link BusinessException}（提示支持的字段集合）。</li>
     * </ul>
     *
     * @param field     前端传入的排序字段，可为空
     * @param whitelist 字段名 → 列名的白名单映射（由调用方维护）
     * @return 数据库列名，或 {@code null}
     */
    public static String resolveField(String field, Map<String, String> whitelist) {
        if (!StringUtils.hasText(field)) {
            return null;
        }
        String column = whitelist.get(field.trim());
        if (column == null) {
            throw new BusinessException(String.format(
                    DictConstants.MSG_SORT_FIELD_INVALID, field, supportedFields(whitelist)));
        }
        return column;
    }

    /**
     * 以确定性顺序（升序）输出白名单支持的字段名，用于错误提示
     */
    private static String supportedFields(Map<String, String> whitelist) {
        Set<String> sorted = new TreeSet<>(whitelist.keySet());
        return sorted.toString();
    }

    /**
     * 便捷方法：返回不可变白名单，便于调用方在 static 块中构建
     */
    public static Map<String, String> unmodifiableWhitelist(Map<String, String> map) {
        return Collections.unmodifiableMap(map);
    }
}
