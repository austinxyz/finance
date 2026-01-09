package com.finance.app.exception;

/**
 * 未授权异常（401 Unauthorized）
 * 当用户未登录或Token无效时抛出
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}
