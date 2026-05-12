package com.junction.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 限流与风控配置。
 *
 * 支持通过 application.yml 按环境配置：
 * - 测试环境：放宽限制便于压测
 * - 生产环境：严格限制防止滥用
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitConfig {

    // ========== IP 级限流 ==========

    /** IP 限流：每秒最大请求数 */
    private int ipLimitPerSecond = 10;

    /** IP 限流：时间窗口（秒） */
    private int ipWindowSeconds = 1;

    // ========== 用户级限流（业务接口可自定义阈值） ==========

    /** 用户限流：每分钟最大请求数 */
    private int userLimitPerMinute = 60;

    /** 用户限流：时间窗口（秒） */
    private int userWindowSeconds = 60;

    // ========== 风控：IP 注册检测 ==========

    /** 单 IP 每小时最大注册数 */
    private int maxRegistrationsPerIpPerHour = 10;

    /** IP 注册计数窗口（秒） */
    private int registrationWindowSeconds = 3600;

    // ========== 风控：自动标记作弊者 ==========

    /** 触发限流多少次后自动标记为作弊者 */
    private int autoCheaterThreshold = 20;

    /** 限流触发计数窗口（秒） */
    private int rateLimitCountWindowSeconds = 3600;
}
