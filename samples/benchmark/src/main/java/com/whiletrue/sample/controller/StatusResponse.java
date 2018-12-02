package com.whiletrue.sample.controller;

import com.whiletrue.ct4j.scheduler.ExecutionStats;
import com.whiletrue.ct4j.tasks.ResourceUsage;

import java.util.Map;

public class StatusResponse {
    private final Map<String, ExecutionStats> performanceSnapshot;
    private final ResourceUsage freeResourcesEstimate;

    public StatusResponse(Map<String, ExecutionStats> performanceSnapshot, ResourceUsage freeResourcesEstimate) {

        this.performanceSnapshot = performanceSnapshot;
        this.freeResourcesEstimate = freeResourcesEstimate;
    }

    public Map<String, ExecutionStats> getPerformanceSnapshot() {
        return performanceSnapshot;
    }

    public ResourceUsage getFreeResourcesEstimate() {
        return freeResourcesEstimate;
    }
}
