package com.junction.config;

import com.junction.interceptor.ApiKeyInterceptor;
import com.junction.interceptor.AuthInterceptor;
import com.junction.interceptor.RateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 拦截器注册。
 *
 * 顺序非常关键：
 * 1) AuthInterceptor：登录鉴权，写入 UserContext
 * 2) RateLimitInterceptor：限流与作弊拦截（依赖 userId）
 * 3) ApiKeyInterceptor：内部接口独立鉴权（不走 Auth）
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;
    private final ApiKeyInterceptor apiKeyInterceptor;

    public WebMvcConfig(AuthInterceptor authInterceptor,
                        RateLimitInterceptor rateLimitInterceptor,
                        ApiKeyInterceptor apiKeyInterceptor) {
        this.authInterceptor = authInterceptor;
        this.rateLimitInterceptor = rateLimitInterceptor;
        this.apiKeyInterceptor = apiKeyInterceptor;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 1. 认证拦截器（排除登录、内部接口和 Swagger）
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/internal/**",
                        "/swagger-ui/**",
                        "/v3/api-docs/**"
                )
                .order(1);

        // 2. 限流拦截器（排除内部接口）
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/internal/**")
                .order(2);

        // 3. API Key 拦截器（仅内部接口）
        registry.addInterceptor(apiKeyInterceptor)
                .addPathPatterns("/api/internal/**")
                .order(3);
    }
}
