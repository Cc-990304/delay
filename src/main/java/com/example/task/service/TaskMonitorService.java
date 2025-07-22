package com.example.task.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TaskMonitorService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public Map<String, Object> getTaskStatus(String taskName) {
        Map<String, Object> status = new HashMap<>();

        String executionKey = "task:execution:" + taskName;
        String lastExecutionTimeKey = "task:lastExecutionTime:" + taskName;
        String successCountKey = "task:successCount:" + taskName;
        String failureCountKey = "task:failureCount:" + taskName;

        // 获取任务执行信息
        Map<String, Object> executionInfo = redisTemplate.opsForHash().entries(executionKey)
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        entry -> String.valueOf(entry.getKey()),
                        entry -> entry.getValue()
                ));
        status.putAll(executionInfo);

        // 获取最后执行时间
        Object lastExecutionTime = redisTemplate.opsForValue().get(lastExecutionTimeKey);
        status.put("lastExecutionTime", lastExecutionTime);

        // 获取成功和失败次数
        Object successCount = redisTemplate.opsForValue().get(successCountKey);
        Object failureCount = redisTemplate.opsForValue().get(failureCountKey);
        status.put("successCount", successCount != null ? successCount : 0);
        status.put("failureCount", failureCount != null ? failureCount : 0);

        return status;
    }
}