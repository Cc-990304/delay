package com.example.task.config;

import com.example.task.annotation.RedisScheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;

import jakarta.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.Map;

@Configuration
@EnableScheduling
public class RedisScheduledTaskRegistrar implements ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(RedisScheduledTaskRegistrar.class);

    private ApplicationContext applicationContext;

    @Autowired
    private TaskScheduler taskScheduler;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);
        scheduler.setThreadNamePrefix("redis-scheduled-");
        scheduler.initialize();
        return scheduler;
    }

    @PostConstruct
    public void init() {
        logger.info("开始初始化 RedisScheduled 任务...");

        // 等待Spring容器完全初始化
        try {
            Thread.sleep(1000); // 给容器一点时间完成初始化
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 扫描所有Bean，不限于@Component注解
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        logger.info("扫描到的 Bean 总数: {}", beanNames.length);

        int taskCount = 0;
        for (String beanName : beanNames) {
            try {
                Object bean = applicationContext.getBean(beanName);
                Class<?> clazz = bean.getClass();

                // 处理 CGLIB 代理
                if (clazz.getName().contains("$$")) {
                    clazz = clazz.getSuperclass();
                }

                Method[] methods = clazz.getDeclaredMethods();
                for (Method method : methods) {
                    RedisScheduled annotation = method.getAnnotation(RedisScheduled.class);
                    if (annotation != null) {
                        registerScheduledTask(bean, method, annotation);
                        taskCount++;
                        logger.info("注册 RedisScheduled 任务: {} (Bean: {})", annotation.name(), beanName);
                    }
                }
            } catch (Exception e) {
                logger.debug("跳过 Bean: {} ({})", beanName, e.getMessage());
            }
        }

        logger.info("RedisScheduled 任务注册完成，共注册 {} 个任务", taskCount);
    }

    private void registerScheduledTask(Object bean, Method method, RedisScheduled annotation) {
        Runnable task = () -> {
            try {
                logger.debug("准备执行 RedisScheduled 任务: {}", annotation.name());
                method.setAccessible(true); // 确保方法可访问
                method.invoke(bean);
                logger.debug("RedisScheduled 任务执行完成: {}", annotation.name());
            } catch (Exception e) {
                logger.error("执行 RedisScheduled 任务失败: {}", annotation.name(), e);
            }
        };

        try {
            CronTrigger trigger = new CronTrigger(annotation.cron());
            taskScheduler.schedule(task, trigger);
            logger.info("成功注册定时任务: {}，Cron: {}", annotation.name(), annotation.cron());
        } catch (Exception e) {
            logger.error("注册定时任务失败: {}", annotation.name(), e);
        }
    }
}