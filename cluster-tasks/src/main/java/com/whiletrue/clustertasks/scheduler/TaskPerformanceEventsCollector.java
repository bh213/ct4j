package com.whiletrue.clustertasks.scheduler;

import com.whiletrue.clustertasks.tasks.Task;
import com.whiletrue.clustertasks.tasks.TaskWrapper;

import java.time.Instant;
import java.util.HashMap;

public class TaskPerformanceEventsCollector implements InternalTaskEvents {

    private HashMap<Class<? extends Task>, ExecutionStats> taskTiming = new HashMap<>();
    private ExecutionStats totalStats = new ExecutionStats();

    @Override
    public void taskStarted(TaskWrapper<?> taskWrapper) {
    }

    @Override
    public synchronized void taskCompleted(TaskWrapper<?> taskWrapper, int retry, float durationMilliseconds) {
        final Class<? extends Task>  name = taskWrapper.getTask().getClass();
        taskTiming.putIfAbsent(name, new ExecutionStats());
        taskTiming.compute(name, (s, executionStats) -> {
            executionStats.taskCompleted();
            return executionStats;
        });
        totalStats.taskCompleted();
    }

    @Override
    public synchronized void taskError(TaskWrapper<?> taskWrapper, int retry, float durationMilliseconds) {
        final Class<? extends Task>  name = taskWrapper.getTask().getClass();
        taskTiming.putIfAbsent(name, new ExecutionStats());
        taskTiming.compute(name, (s, executionStats) -> {
            executionStats.taskError();
            return executionStats;
        });
        totalStats.taskError();

    }

    @Override
    public synchronized void taskFailed(TaskWrapper<?> taskWrapper, int retry) {
        final Class<? extends Task>  name = taskWrapper.getTask().getClass();
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
