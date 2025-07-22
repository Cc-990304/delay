package com.example.task.service;

import com.example.task.annotation.RedisScheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SampleTasks {

    private static final Logger logger = LoggerFactory.getLogger(SampleTasks.class);

    public SampleTasks() {
        logger.info("SampleTasks 实例已创建");
    }

    @RedisScheduled(name = "每两秒执行一次", cron = "*/2 * * * * *", lockExpire = 120)
    public void sampleTask() {
        logger.info("执行示例任务: {}", System.currentTimeMillis());
        System.out.println("执行示例任务: " + System.currentTimeMillis());
    }

//    // 添加一个普通的Spring @Scheduled任务用于对比测试
//    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 5000)
//    public void testSpringScheduled() {
//        logger.info("Spring原生定时任务执行: {}", System.currentTimeMillis());
//        System.out.println("Spring原生定时任务执行: " + System.currentTimeMillis());
//    }
}