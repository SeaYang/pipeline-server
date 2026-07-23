package com.ci.pipeline.common.auth;

/**
 * 当前登录用户上下文。
 *
 * <p>基于 {@link ThreadLocal} 在一次请求线程内持有用户标识（来自请求头 {@code x-user-id}），
 * 由 {@link UserIdFilter} 在请求开始时写入、请求结束时清除。
 * 业务代码可通过静态方法 {@link #getUserId()} 方便地获取当前登录用户。</p>
 */
public final class UserContext {

    private static final ThreadLocal<String> USER_ID_HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    /**
     * 设置当前线程的用户 ID（供 Filter 调用）。
     *
     * @param userId 用户 ID
     */
    public static void setUserId(String userId) {
        USER_ID_HOLDER.set(userId);
    }

    /**
     * 获取当前线程的用户 ID。
     *
     * @return 用户 ID，未登录或未传递 {@code x-user-id} 时返回 {@code null}
     */
    public static String getUserId() {
        return USER_ID_HOLDER.get();
    }

    /**
     * 是否已登录（用户 ID 非空且非空白）。
     *
     * @return 已登录返回 true
     */
    public static boolean isLogin() {
        String userId = USER_ID_HOLDER.get();
        return userId != null && !userId.trim().isEmpty();
    }

    /**
     * 清除当前线程的用户 ID（供 Filter 在 finally 中调用，避免线程复用导致的内存泄漏与串号）。
     */
    public static void clear() {
        USER_ID_HOLDER.remove();
    }
}
