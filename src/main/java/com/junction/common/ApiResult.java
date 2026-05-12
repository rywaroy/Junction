package com.junction.common;

import lombok.Data;

/**
 * 统一 API 返回结构。
 *
 * 约定：
 * - code = 0 表示成功
 * - code != 0 表示业务失败或系统失败
 * - message 为错误提示或固定 success
 */
@Data
public class ApiResult<T> {

    private int code;
    private String message;
    private T data;

    private ApiResult(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(0, "success", data);
    }

    public static <T> ApiResult<T> success() {
        return new ApiResult<>(0, "success", null);
    }

    public static <T> ApiResult<T> error(int code, String message) {
        return new ApiResult<>(code, message, null);
    }

    public static <T> ApiResult<T> error(String message) {
        return new ApiResult<>(-1, message, null);
    }
}
