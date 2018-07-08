package com.whiletrue.clustertasks.scheduler;

import com.whiletrue.clustertasks.tasks.Task;

import java.time.Instant;
import java.util.HashMap;

public class TaskPerformanceEventsCollector implements InternalTaskEvents {

    private HashMap<Class<? extends Task>, ExecutionStats> taskTiming = new HashMap<>();
    private ExecutionStats totalStats = new ExecutionStats();

    @Override
    public void taskStarted(Class<? extends Task> name, String id) {
    }

    @Override
    public synchronized void taskCompleted(Class<? extends Task> name, String id, int retry, float milliseconds) {
        taskTiming.putIfAbsent(name, new ExecutionStats());
        taskTiming.compute(name, (s, executionStats) -> {
            executionStats.taskCompleted();
            return executionStats;
        });
        totalStats.taskCompleted();
    }

    @Override
    public synchronized void taskError(Class<? extends Task> name, String id, int retry, float milliseconds) {
        taskTiming.putIfAbsent(name, new ExecutionStats());
        taskTiming.compute(name, (s, executionStats) -> {
            executionStats.taskError();
            return executionStats;
        });
        totalStats.taskError();

    }

    @Override
    public synchronized void taskFailed(Class<? extends Task> name, String id, int retry) {
        taskTiming.putIfAbsent(name, new ExecutionStats());
        taskTiming.compute(name, (s, executionStats) -> {
            executionStats.taskFailed();
            return executionStats;
        });
        totalStats.taskFailed();
    }

    public synchronized TaskPerformanceStatsSnapshot getPerformanceSnapshot() {
        return new TaskPerformanceStatsSnapshot(taskTiming, totalStats, Instant.now());
    }

}
