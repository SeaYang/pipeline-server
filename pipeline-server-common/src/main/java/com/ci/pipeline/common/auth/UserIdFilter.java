package com.ci.pipeline.common.auth;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 用户 ID 过滤器。
 *
 * <p>从请求头 {@code x-user-id} 中提取登录用户 ID 并写入 {@link UserContext}，
 * 供后续业务代码（含 {@link LoginAspect}）使用。</p>
 *
 * <p>关键点：无论业务是否抛异常，都必须在 {@code finally} 中清除 ThreadLocal，
 * 避免在 Tomcat 线程复用场景下出现用户串号与内存泄漏。</p>
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UserIdFilter extends OncePerRequestFilter {

    /**
     * 登录用户 ID 的请求头名称。
     */
    public static final String HEADER_USER_ID = "x-user-id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 优先从请求头获取；header 没有时回退到 query param（用于 EventSource 等 无法自定义 header 的场景）
            String userId = request.getHeader(HEADER_USER_ID);
            if (StringUtils.isBlank(userId)) {
                userId = request.getParameter(HEADER_USER_ID);
            }
            if (StringUtils.isNotBlank(userId)) {
                UserContext.setUserId(userId);
            }
            filterChain.doFilter(request, response);
        } finally {
            // 确保线程复用前清理，避免串号与内存泄漏
            UserContext.clear();
        }
    }
}
