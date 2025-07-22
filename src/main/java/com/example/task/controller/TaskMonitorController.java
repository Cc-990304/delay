package com.example.task.controller;

import com.example.task.service.TaskMonitorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
public class TaskMonitorController {

    @Autowired
    private TaskMonitorService taskMonitorService;

    @GetMapping("/{taskName}/status")
    public Map<String, Object> getTaskStatus(@PathVariable String taskName) {
        return taskMonitorService.getTaskStatus(taskName);
    }
}