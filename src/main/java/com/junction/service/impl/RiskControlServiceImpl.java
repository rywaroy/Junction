package com.junction.service.impl;

import com.junction.config.RateLimitConfig;
import com.junction.service.RiskControlService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 风控服务实现。
 */
@Slf4j
@Service
public class RiskControlServiceImpl implements RiskControlService {

    private static final String IP_REGISTRATION_PREFIX = "risk:ip:reg:";
    private static final String CHEATER_PREFIX = "risk:cheater:";
    private static final String RATE_LIMIT_COUNT_PREFIX = "risk:ratelimit:count:";

    private final StringRedisTemplate redisTemplate;
    private final RateLimitConfig rateLimitConfig;

    public RiskControlServiceImpl(StringRedisTemplate redisTemplate, RateLimitConfig rateLimitConfig) {
        this.redisTemplate = redisTemplate;
        this.rateLimitConfig = rateLimitConfig;
    }

    @Override
    public boolean isAbnormalIp(String ip) {
        String key = IP_REGISTRATION_PREFIX + ip;
        String val = redisTemplate.opsForValue().get(key);
        int count = val != null ? Integer.parseInt(val) : 0;
        return count >= rateLimitConfig.getMaxRegistrationsPerIpPerHour();
    }

    @Override
    public void recordRegistration(String ip, Long userId) {
        String key = IP_REGISTRATION_PREFIX + ip;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, rateLimitConfig.getRegistrationWindowSeconds(), TimeUnit.SECONDS);
        }
        if (count != null && count > rateLimitConfig.getMaxRegistrationsPerIpPerHour()) {
            log.warn("Abnormal registration detected: IP={}, count={}, userId={}", ip, count, userId);
        }
    }

    @Override
    public void markAsCheater(Long userId, String reason) {
        String key = CHEATER_PREFIX + userId;
        redisTemplate.opsForValue().set(key, reason);
        log.warn("User marked as cheater: userId={}, reason={}", userId, reason);
    }

    @Override
    public boolean isCheater(Long userId) {
        String key = CHEATER_PREFIX + userId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    @Override
    public void recordRateLimitTrigger(Long userId, String triggerType) {
        String key = RATE_LIMIT_COUNT_PREFIX + userId;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, rateLimitConfig.getRateLimitCountWindowSeconds(), TimeUnit.SECONDS);
        }
        log.info("Rate limit triggered for user {}: type={}, count={}", userId, triggerType, count);

        if (count != null && count >= rateLimitConfig.getAutoCheaterThreshold()) {
            if (!isCheater(userId)) {
                markAsCheater(userId, "频繁触发限流(" + count + "次)");
            }
        }
    }

    @Override
    public void unmarkCheater(Long userId) {
        String key = CHEATER_PREFIX + userId;
        redisTemplate.delete(key);
        log.info("User unmarked as cheater: userId={}", userId);
    }
}
