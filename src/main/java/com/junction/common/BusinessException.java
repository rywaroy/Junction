package com.junction.common;

import lombok.Getter;

/**
 * 业务异常。
 *
 * 用于表示"可预期"的业务校验失败（如未登录、参数非法等）。
 * 由 GlobalExceptionHandler 统一转换成 ApiResult 返回给前端。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = -1;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
