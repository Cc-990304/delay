package com.example.task.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RedisScheduled {

    /**
     * 任务名称，需保持唯一
     */
    String name();

    /**
     * cron表达式
     */
    String cron();

    /**
     * 锁过期时间(秒)
     */
    int lockExpire() default 60;

    /**
     * 是否开启分布式锁
     */
    boolean enableLock() default true;
}