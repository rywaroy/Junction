package com.junction.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

/**
 * 定时任务示例。
 *
 * 演示要点：
 * - @Scheduled cron 表达式
 * - 通过 Redis setIfAbsent 实现幂等锁，避免多实例重复执行
 *
 * 模板项目可保留作为脚手架，业务方按需新增。
 */
@Slf4j
@Component
public class SampleScheduler {

    private static final String EXECUTED_KEY_PREFIX = "scheduler:sample:executed:";

    private final StringRedisTemplate redisTemplate;

    public SampleScheduler(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /** 每天 03:00 执行一次（避开高峰） */
    @Scheduled(cron = "0 0 3 * * ?")
    public void dailyJob() {
        LocalDate today = LocalDate.now();
        String executedKey = EXECUTED_KEY_PREFIX + today;

        Boolean firstRun = redisTemplate.opsForValue().setIfAbsent(executedKey, "1", 7, TimeUnit.DAYS);
        if (Boolean.FALSE.equals(firstRun)) {
            log.info("Sample scheduler already executed for {}, skipping", today);
            return;
        }

        try {
            log.info("Sample scheduler running for {}", today);
            // TODO: 业务逻辑写这里
        } catch (Exception e) {
            log.error("Sample scheduler failed", e);
        }
    }
}
