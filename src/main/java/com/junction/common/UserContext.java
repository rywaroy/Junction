package com.junction.common;

/**
 * 当前登录用户上下文（ThreadLocal）。
 *
 * - AuthInterceptor 在鉴权通过后写入 userId
 * - 业务代码通过 UserContext.getUserId() 获取当前用户
 * - 请求结束后必须 clear，防止线程复用导致脏数据（见 AuthInterceptor.afterCompletion）
 */
public class UserContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    public static void setUserId(Long userId) {
        USER_ID.set(userId);
    }

    public static Long getUserId() {
        return USER_ID.get();
    }

    public static void clear() {
        USER_ID.remove();
    }
}
