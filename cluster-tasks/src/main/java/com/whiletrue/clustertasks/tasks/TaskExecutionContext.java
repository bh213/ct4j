package com.whiletrue.clustertasks.tasks;

import com.whiletrue.clustertasks.tasks.recurring.RecurringSchedule;

import java.util.Optional;

/**
 * Task execution data that is passed to task's run method
 */
public class TaskExecutionContext {
    private int retry;
    private String clusterNodeId;
    private String taskId;
    private String taskName;
    private Long startTime;
    private Long endTime;
    private Optional<RecurringSchedule> recurringSchedule;


    public TaskExecutionContext(int retry, String clusterNodeId, String taskId, String taskName, RecurringSchedule recurringScheduleStrategy) {
        this.retry = retry;
        this.clusterNodeId = clusterNodeId;
        this.taskId = taskId;
        this.taskName = taskName;
        this.startTime = null;
        this.endTime = null;
        this.recurringSchedule = Optional.ofNullable(recurringScheduleStrategy);
    }

    public Optional<RecurringSchedule> getRecurringSchedule() {
        return recurringSchedule;
    }

    public int getRetry() {
        return retry;
    }

    public String getClusterNodeId() {
        return clusterNodeId;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getTaskName() {
        return taskName;
    }


    public long getRunningTimeInMs() {
        if (startTime == null) return 0;
        if (endTime == null) {
            return (System.nanoTime() - this.startTime)/1000000;
        }
        else return (endTime - startTime) / 1000000;

    }

    void startTimer() {
        this.startTime = System.nanoTime();
    }
    long stopTimer() {
        this.endTime = System.nanoTime();
        return (endTime - startTime)/1000000;
    }
}
