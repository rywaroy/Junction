package com.junction.service;

/**
 * 风控服务。
 *
 * 关注两类风险：
 * - IP 维度：短时间大量注册（识别批量刷号）
 * - 用户维度：频繁触发限流（识别脚本高频请求）
 */
public interface RiskControlService {

    /** 判断 IP 是否在注册风控阈值之内（true = 异常） */
    boolean isAbnormalIp(String ip);

    /** 记录一次注册（同时按 IP 计数） */
    void recordRegistration(String ip, Long userId);

    /** 标记用户为作弊者 */
    void markAsCheater(Long userId, String reason);

    /** 用户是否被标记为作弊者 */
    boolean isCheater(Long userId);

    /** 记录一次限流触发；超过阈值会自动标记为作弊者 */
    void recordRateLimitTrigger(Long userId, String triggerType);

    /** 解除作弊标记 */
    void unmarkCheater(Long userId);
}
