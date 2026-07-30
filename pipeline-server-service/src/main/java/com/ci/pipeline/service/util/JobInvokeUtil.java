package com.ci.pipeline.service.util;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ClassUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * 反射调用 Spring Bean 方法的工具类，供定时任务执行时按 bean_name/method_name/method_params 反射调用目标方法。
 *
 * <p><b>权限说明</b>：本工具类不做 bean_name 白名单或调用权限校验——任何在 Spring 容器中可被
 * {@code applicationContext.getBean(beanName)} 取到的 Bean 及其 public 方法都可以被配置执行，
 * 见 docs/techdesign/cron-job-design.md 6.1 节说明，这是当前阶段的已知取舍，非遗漏。
 */
public final class JobInvokeUtil {

    private JobInvokeUtil() {
    }

    /**
     * 反射调用 Spring Bean 方法。
     *
     * @param ctx          Spring ApplicationContext
     * @param beanName     Bean 名称
     * @param methodName   方法名称
     * @param methodParams 方法参数 JSON 数组字符串，如 ["daily", 500, true]；无参传 null/空字符串
     * @throws Exception 目标方法执行过程中抛出的异常（已从 {@link InvocationTargetException} 中解包，
     *                    以便调用方能按原始异常类型精确处理，例如 InterruptedException）
     */
    public static void invokeMethod(ApplicationContext ctx, String beanName,
                                     String methodName, String methodParams) throws Exception {
        Object bean = resolveBean(ctx, beanName);
        Object[] paramValues = parseParamValues(methodParams);
        Method method = resolveMethod(bean.getClass(), beanName, methodName, paramValues);
        try {
            method.invoke(bean, paramValues);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw e;
        }
    }

    /**
     * 仅校验 Bean 和方法是否存在（不实际调用），供新增/编辑任务时做前置校验。
     *
     * @throws IllegalStateException Bean 不存在，或找不到匹配的方法签名
     */
    public static void validateBeanMethod(ApplicationContext ctx, String beanName,
                                           String methodName, String methodParams) {
        Object bean = resolveBean(ctx, beanName);
        Object[] paramValues = parseParamValues(methodParams);
        resolveMethod(bean.getClass(), beanName, methodName, paramValues);
    }

    private static Object resolveBean(ApplicationContext ctx, String beanName) {
        if (!ctx.containsBean(beanName)) {
            throw new IllegalStateException("Bean不存在[" + beanName + "]");
        }
        return ctx.getBean(beanName);
    }

    private static Object[] parseParamValues(String methodParams) {
        if (StringUtils.isBlank(methodParams)) {
            return new Object[0];
        }
        List<Object> paramList = JSON.parseArray(methodParams, Object.class);
        return paramList.toArray();
    }

    private static Method resolveMethod(Class<?> clazz, String beanName, String methodName, Object[] paramValues) {
        Class<?>[] paramTypes = new Class<?>[paramValues.length];
        for (int i = 0; i < paramValues.length; i++) {
            paramTypes[i] = paramValues[i].getClass();
        }
        Method method = findMethod(clazz, methodName, paramTypes);
        if (method == null) {
            throw new IllegalStateException(
                    "方法不存在[" + beanName + "." + methodName + Arrays.toString(paramTypes) + "]");
        }
        return method;
    }

    /**
     * 按方法名和实参类型查找方法。
     * <p><b>注意</b>：不能用 {@code clazz.getMethod(name, paramTypes)} 精确匹配——JSON 解析出的参数
     * 类型是包装类型（Integer/Boolean...），若目标方法参数声明为基本类型（int/boolean...），
     * 精确匹配会直接抛出 NoSuchMethodException。这里改为遍历比较，用
     * {@link ClassUtils#resolvePrimitiveIfNecessary} 把基本类型转换为包装类型后再判断可赋值性，
     * 兼容两种参数声明方式。
     */
    private static Method findMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
        for (Method method : clazz.getMethods()) {
            if (!method.getName().equals(methodName)) {
                continue;
            }
            Class<?>[] declaredTypes = method.getParameterTypes();
            if (declaredTypes.length != paramTypes.length) {
                continue;
            }
            boolean match = true;
            for (int i = 0; i < declaredTypes.length; i++) {
                Class<?> resolvedDeclared = ClassUtils.resolvePrimitiveIfNecessary(declaredTypes[i]);
                if (!resolvedDeclared.isAssignableFrom(paramTypes[i])) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return method;
            }
        }
        return null;
    }
}
