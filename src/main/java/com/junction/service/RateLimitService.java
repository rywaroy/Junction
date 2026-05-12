package com.junction.service;

/**
 * 限流服务。
 *
 * 通用 API：调用方传 key、阈值、窗口秒数。
 */
public interface RateLimitService {

    /**
     * 是否触发限流。
     *
     * @param key            限流 key（不带前缀，建议用业务维度组合，例如 "ip:1.2.3.4"、"user:123"）
     * @param limit          阈值
     * @param windowSeconds  时间窗口（秒）
     * @return true 表示已触发限流
     */
    boolean isRateLimited(String key, int limit, int windowSeconds);

    /**
     * 当前窗口内的计数。
     */
    int getCurrentCount(String key, int windowSeconds);
}
