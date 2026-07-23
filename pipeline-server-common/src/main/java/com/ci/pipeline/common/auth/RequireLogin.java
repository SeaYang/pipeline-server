package com.ci.pipeline.common.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 登录态校验注解。
 *
 * <p>底层由 {@link LoginAspect}（AOP）拦截，校验当前请求的 {@code x-user-id} 不为空，否则抛出
 * {@code UnauthorizedException}。</p>
 *
 * <p>支持加在类或方法上，优先级规则：</p>
 * <ul>
 *     <li>方法上有 {@code @RequireLogin}：以方法上的为准（方法级覆盖类级）；</li>
 *     <li>方法上没有、但类上有：继承类上的配置；</li>
 *     <li>{@code @RequireLogin(false)} 表示显式关闭校验，可用于在“类上要求登录”时放开个别方法。</li>
 * </ul>
 *
 * <p>典型用法：</p>
 * <pre>
 * &#64;RequireLogin              // 整个 Controller 都需要登录
 * &#64;RestController
 * public class OrderController {
 *     &#64;GetMapping("/public")  // 单独放开，无需登录
 *     &#64;RequireLogin(false)
 *     public Result&lt;?&gt; pub() { ... }
 * }
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireLogin {

    /**
     * 是否需要登录态校验，默认开启。
     *
     * @return 需要校验返回 true
     */
    boolean value() default true;
}
