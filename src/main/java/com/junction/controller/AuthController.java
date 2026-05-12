package com.junction.controller;

import com.junction.common.ApiResult;
import com.junction.dto.LoginRequest;
import com.junction.dto.LoginResponse;
import com.junction.interceptor.RateLimitInterceptor;
import com.junction.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证管理", description = "登录相关接口")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "用户登录", description = "微信小程序登录，返回会话 token")
    @PostMapping("/login")
    public ApiResult<LoginResponse> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String clientIp = RateLimitInterceptor.getClientIp(httpRequest);
        return ApiResult.success(authService.login(request, clientIp));
    }
}
