package com.ci.pipeline.common.util;

/**
 * 路径处理工具类。
 * <p>提供构建上下文路径、模块路径等场景下的相对路径转换。
 */
public final class PathUtil {

    private PathUtil() {
    }

    /**
     * 将任意路径归一化为 Dockerfile CONTEXT / MODULE 所需的相对路径形式。
     * <p>核心思路：先剥掉所有前导 {@code ./}，再统一补一个回去。
     *
     * @param raw 原始路径，可为 null
     * @return 形如 {@code ./} 或 {@code ./a/b/c} 的相对路径
     */
    public static String toRelativePath(String raw) {
        if (raw == null || raw.trim().isEmpty() || ".".equals(raw.trim())) {
            return "./";
        }
        // 去掉前导 ./ 或 /，循环处理用户可能写了多个的情况（如 ././/cmd）
        String path = raw.trim();
        while (path.startsWith("./") || path.startsWith("/")) {
            path = path.replaceFirst("^\\./|^/", "");
        }
        return "./" + path;
    }
}
