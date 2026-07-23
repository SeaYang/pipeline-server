package com.ci.pipeline.common.exception;

/**
 * 未登录 / 权限不足异常。
 *
 * <p>用于登录态校验失败等场景，约定业务码为 401。</p>
 */
public class UnauthorizedException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public UnauthorizedException(String message) {
        super(401, message);
    }
}
