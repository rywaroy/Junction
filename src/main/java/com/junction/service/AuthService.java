package com.junction.service;

import com.junction.dto.LoginRequest;
import com.junction.dto.LoginResponse;

public interface AuthService {

    /**
     * 微信小程序登录。
     *
     * - 新用户自动创建（带 IP 风控）
     * - 可选：通过 phoneCode 拉取手机号
     * - 返回 token，存入 Redis（7 天过期）
     */
    LoginResponse login(LoginRequest request, String clientIp);
}
