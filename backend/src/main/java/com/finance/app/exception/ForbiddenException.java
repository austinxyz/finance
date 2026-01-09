package com.finance.app.exception;

/**
 * 禁止访问异常（403 Forbidden）
 * 当用户已登录但无权限访问资源时抛出
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }

    public ForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }
}
