package com.whiletrue.sample.controller;

import com.whiletrue.clustertasks.scheduler.ExecutionStats;
import com.whiletrue.clustertasks.tasks.ResourceUsage;
import com.whiletrue.clustertasks.tasks.TaskManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class StatusController {

    private final TaskManager taskManager;

    @Autowired
    public StatusController(TaskManager taskManager) {
        this.taskManager = taskManager;

    }

    @GetMapping(path = "/", produces = "application/json")
    public StatusResponse getStatus() {
        final Map<String, ExecutionStats> performanceSnapshot = taskManager.getPerformanceSnapshot();
        final ResourceUsage resourceUsage = taskManager.getFreeResourcesEstimate();
        return new StatusResponse(performanceSnapshot,resourceUsage );



    }
}
