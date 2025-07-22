package com.example.task;

import com.example.task.annotation.RedisScheduled;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@SpringBootApplication
@EnableScheduling

public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

//@Component
//class SampleTasks {
//
//    @RedisScheduled(name = "sampleTask", cron = "*/5 * * * * *", lockExpire = 120)
//    public void sampleTask() {
//        System.out.println("执行示例任务: " + System.currentTimeMillis());
//        System.out.println("执行定时任务");
//        // 这里是定时执行的业务逻辑
//    }


//@Component // 确保 Spring 能扫描到这个组件
//class Test {
//
//    @Scheduled(fixedRate = 3000) // 每3秒执行一次
//    public  void testTask() {
//        System.out.println("测试任务执行: " + System.currentTimeMillis());
//    }
//}