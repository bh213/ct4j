package com.whiletrue.clustertasks.tasks;

import com.whiletrue.clustertasks.factory.ClusterTasksCustomFactory;
import com.whiletrue.clustertasks.scheduler.ExecutionStats;
import com.whiletrue.clustertasks.scheduler.TaskPerformanceStatsInterval;
import com.whiletrue.clustertasks.scheduler.TaskPerformanceStatsSnapshot;

import java.util.Map;

public interface TaskManager {
    <INPUT, TASK extends Task<INPUT>> String queueTask(Class<TASK> taskClass, INPUT input) throws Exception;
    <INPUT, TASK extends Task<INPUT>> String queueTask(Class<TASK> taskClass, INPUT input, int priority) throws Exception;

    <INPUT, TASK extends Task<INPUT>> String queueTaskDelayed(Class<TASK> taskClass, INPUT input, long millisecondStartDelay) throws Exception;
    <INPUT, TASK extends Task<INPUT>> String queueTaskDelayed(Class<TASK> taskClass, INPUT input, long millisecondStartDelay, int priority) throws Exception;

    void stopScheduling();
    void startScheduling();
    long countPendingTasks();

    ResourceUsage getFreeResourcesEstimate();

    void addCustomTaskFactory(ClusterTasksCustomFactory customTaskFactory);
    void remoteCustomTaskFactory(ClusterTasksCustomFactory customTaskFactory);

    TaskPerformanceStatsInterval getPerformanceInterval(TaskPerformanceStatsSnapshot start, TaskPerformanceStatsSnapshot end);

    Map<String, ExecutionStats> getPerformanceSnapshot();
}

