package com.example.task.aspect;

import com.example.task.annotation.RedisScheduled;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
public class RedisScheduledAspect {

    public static final Logger logger = LoggerFactory.getLogger(RedisScheduledAspect.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final RedisScript<Long> UNLOCK_SCRIPT;

    static {
        StringBuilder script = new StringBuilder();
        script.append("if redis.call('get', KEYS[1]) == ARGV[1] then ");
        script.append("    return redis.call('del', KEYS[1]) ");
        script.append("else ");
        script.append("    return 0 ");
        script.append("end");
        UNLOCK_SCRIPT = new DefaultRedisScript<>(script.toString(), Long.class);
    }

    @PostConstruct
    public void init() {
        logger.info("RedisScheduledAspect 初始化完成");
    }

    @Around("@annotation(com.example.task.annotation.RedisScheduled)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        logger.info("RedisScheduledAspect 拦截到方法调用");

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RedisScheduled scheduled = method.getAnnotation(RedisScheduled.class);

        logger.info("准备执行任务: {}", scheduled.name());

        String lockKey = "task:lock:" + scheduled.name();
        String lockValue = UUID.randomUUID().toString();
        boolean locked = false;

        try {
            // 获取分布式锁
            if (scheduled.enableLock()) {
                try {
                    locked = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue,
                            scheduled.lockExpire(), TimeUnit.SECONDS);
                    if (!locked) {
                        logger.info("任务[{}]获取锁失败，可能正在其他节点执行", scheduled.name());
                        return null;
                    }
                    logger.info("任务[{}]获取锁成功", scheduled.name());
                } catch (Exception e) {
                    logger.warn("获取分布式锁时发生异常，继续执行任务: {}", e.getMessage());
                    // 如果Redis不可用，继续执行任务
                }
            }

            // 执行定时任务
            long startTime = System.currentTimeMillis();
            logger.info("开始执行任务[{}]", scheduled.name());
            Object result = joinPoint.proceed();
            long endTime = System.currentTimeMillis();

            // 记录任务执行信息
            logger.info("任务[{}]执行完成，耗时: {}ms", scheduled.name(), endTime - startTime);
            recordTaskExecution(scheduled.name(), endTime - startTime, true);

            return result;
        } catch (Exception e) {
            logger.error("任务[{}]执行失败: {}", scheduled.name(), e.getMessage(), e);
            recordTaskExecution(scheduled.name(), 0, false);
            throw e;
        } finally {
            // 释放分布式锁
            if (locked) {
                try {
                    redisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(lockKey), lockValue);
                    logger.info("任务[{}]释放锁", scheduled.name());
                } catch (Exception e) {
                    logger.warn("释放分布式锁时发生异常: {}", e.getMessage());
                }
            }
        }
    }

    private void recordTaskExecution(String taskName, long duration, boolean success) {
        try {
            String executionKey = "task:execution:" + taskName;
            String lastExecutionTimeKey = "task:lastExecutionTime:" + taskName;
            String successCountKey = "task:successCount:" + taskName;
            String failureCountKey = "task:failureCount:" + taskName;

            // 记录执行时间和耗时
            redisTemplate.opsForValue().set(lastExecutionTimeKey, System.currentTimeMillis());
            redisTemplate.opsForHash().put(executionKey, "duration", duration);
            redisTemplate.opsForHash().put(executionKey, "success", success);

            // 更新成功/失败计数
            if (success) {
                redisTemplate.opsForValue().increment(successCountKey);
            } else {
                redisTemplate.opsForValue().increment(failureCountKey);
            }
        } catch (Exception e) {
            logger.warn("记录任务执行信息时发生异常: {}", e.getMessage());
        }
    }
}