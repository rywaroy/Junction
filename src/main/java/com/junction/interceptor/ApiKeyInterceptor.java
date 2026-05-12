package com.junction.interceptor;

import com.junction.config.InternalApiConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 内部接口 API Key 鉴权拦截器。
 *
 * 校验请求头 X-API-Key 与配置值是否匹配。
 * 用于管理后台、报表导出等内部接口。
 */
@Component
public class ApiKeyInterceptor implements HandlerInterceptor {

    private final InternalApiConfig internalApiConfig;

    public ApiKeyInterceptor(InternalApiConfig internalApiConfig) {
        this.internalApiConfig = internalApiConfig;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String apiKey = request.getHeader("X-API-Key");
        String configuredKey = internalApiConfig.getApiKey();

        if (configuredKey.equals(apiKey == null ? "" : apiKey)) {
            return true;
        }

        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"API Key 无效\"}");
        return false;
    }
}
