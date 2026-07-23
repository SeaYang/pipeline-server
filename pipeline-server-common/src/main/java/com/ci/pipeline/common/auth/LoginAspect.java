package com.ci.pipeline.common.auth;

import com.ci.pipeline.common.exception.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 登录态校验切面。
 *
 * <p>拦截类或方法上标注了 {@link RequireLogin} 的 Controller 方法：
 * 若生效配置为开启（{@link RequireLogin#value()} 为 true），
 * 则校验 {@link UserContext#getUserId()} 非空，否则抛出 {@link UnauthorizedException}。</p>
 *
 * <p>方法级注解优先于类级注解，从而支持“类上要求登录、个别方法用 {@code @RequireLogin(false)} 放开”的场景。</p>
 */
@Slf4j
@Aspect
@Component
public class LoginAspect {

    /**
     * 匹配：方法所在类标注了 {@link RequireLogin}，或方法本身标注了 {@link RequireLogin}。
     * 真正是否校验由方法体内根据方法级/类级注解决定，避免方法级 {@code @RequireLogin(false)} 被类级规则误判。
     */
    @Around("@within(com.ci.pipeline.common.auth.RequireLogin) || "
            + "@annotation(com.ci.pipeline.common.auth.RequireLogin)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        RequireLogin effective = resolveEffective(pjp);
        if (effective != null && effective.value() && !UserContext.isLogin()) {
            log.warn("登录态校验失败, target={}", pjp.getSignature().toShortString());
            throw new UnauthorizedException("未登录或登录态已过期");
        }
        return pjp.proceed();
    }

    /**
     * 解析生效的注解：方法级优先，方法级缺失时回退到类级。
     *
     * @param pjp 切点
     * @return 生效的注解；方法与类均无则返回 null
     */
    private RequireLogin resolveEffective(ProceedingJoinPoint pjp) {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        RequireLogin methodAnno = method.getAnnotation(RequireLogin.class);
        if (methodAnno != null) {
            return methodAnno;
        }
        return method.getDeclaringClass().getAnnotation(RequireLogin.class);
    }
}
