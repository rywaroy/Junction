package com.junction.interceptor;

import com.junction.common.UserContext;
import com.junction.config.RateLimitConfig;
import com.junction.service.RateLimitService;
import com.junction.service.RiskControlService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 限流 + 风控拦截器。
 *
 * 分层策略：
 * - IP 级别：限制单位时间请求数，防止接口被打爆
 * - 用户级别：登录后接口的全局兜底限流
 * - 风控联动：频繁触发限流会被自动标记为作弊者
 *
 * 限流阈值通过 application.yml 配置，支持按环境区分。
 * 如需对特定接口加严限流，可在此处按 uri 增加判断。
 */
@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;
    private final RiskControlService riskControlService;
    private final RateLimitConfig rateLimitConfig;

    public RateLimitInterceptor(RateLimitService rateLimitService,
                                RiskControlService riskControlService,
                                RateLimitConfig rateLimitConfig) {
        this.rateLimitService = rateLimitService;
        this.riskControlService = riskControlService;
        this.rateLimitConfig = rateLimitConfig;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String ip = getClientIp(request);

        // 1. IP 级别限流
        if (rateLimitService.isRateLimited("ip:" + ip,
                rateLimitConfig.getIpLimitPerSecond(),
                rateLimitConfig.getIpWindowSeconds())) {
            log.warn("IP rate limited: {}", ip);
            return reject(response, 429, "请求过于频繁，请稍后再试");
        }

        // 2. 用户级别限流（已登录接口才会有 userId）
        Long userId = UserContext.getUserId();
        if (userId != null) {
            // 风控：作弊用户直接拒绝
            if (riskControlService.isCheater(userId)) {
                return reject(response, 403, "账号异常，请联系客服");
            }

            // 用户级兜底限流
            if (rateLimitService.isRateLimited("user:" + userId,
                    rateLimitConfig.getUserLimitPerMinute(),
                    rateLimitConfig.getUserWindowSeconds())) {
                log.warn("User rate limited: userId={}", userId);
                riskControlService.recordRateLimitTrigger(userId, "user");
                return reject(response, 429, "操作过于频繁，请稍后再试");
            }
        }

        return true;
    }

    private boolean reject(HttpServletResponse response, int status, String message) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + status + ",\"message\":\"" + message + "\"}");
        return false;
    }

    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
