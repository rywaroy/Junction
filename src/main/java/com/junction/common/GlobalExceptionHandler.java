package com.junction.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理。
 *
 * - BusinessException：业务异常，直接返回前端可理解的提示
 * - 其他异常：系统异常，返回统一提示，避免泄露内部信息
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ApiResult<?> handleBusinessException(BusinessException e) {
        log.warn("Business exception: {}", e.getMessage());
        return ApiResult.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ApiResult<?> handleException(Exception e) {
        log.error("Unexpected exception", e);
        return ApiResult.error(500, "系统异常，请稍后重试");
    }
}
