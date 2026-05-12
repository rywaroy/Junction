package com.junction.service.impl;

import com.junction.service.RateLimitService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 简易限流实现（Redis 计数器）。
 *
 * - 时间窗口分桶：当前时间戳 / windowSeconds 作为 bucket
 * - 对 bucketKey 执行 INCR，首次写入时设置过期时间 = windowSeconds
 * - count > limit 即认为触发限流
 */
@Service
public class RateLimitServiceImpl implements RateLimitService {

    private static final String RATE_LIMIT_PREFIX = "ratelimit:";

    private final StringRedisTemplate redisTemplate;

    public RateLimitServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean isRateLimited(String key, int limit, int windowSeconds) {
        long bucket = System.currentTimeMillis() / 1000 / windowSeconds;
        String redisKey = RATE_LIMIT_PREFIX + key + ":" + bucket;

        Long count = redisTemplate.opsForValue().increment(redisKey);
        if (count != null && count == 1) {
            redisTemplate.expire(redisKey, windowSeconds, TimeUnit.SECONDS);
        }

        return count != null && count > limit;
    }

    @Override
    public int getCurrentCount(String key, int windowSeconds) {
        long bucket = System.currentTimeMillis() / 1000 / windowSeconds;
        String redisKey = RATE_LIMIT_PREFIX + key + ":" + bucket;
        String val = redisTemplate.opsForValue().get(redisKey);
        return val != null ? Integer.parseInt(val) : 0;
    }
}
