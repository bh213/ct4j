package com.whiletrue.ct4j.tasks;

public class BasicTaskInfo {
    private final long runningTimeInMs;
    private final int maxRunningTimeInMilliseconds;
    private final int retry;
    private final String taskName;
    private String taskId;
    private Class taskClass;

    public BasicTaskInfo(TaskWrapper<?> taskWrapper) {
        this.taskId = taskWrapper.getTaskExecutionContext().getTaskId();
        this.retry = taskWrapper.getTaskExecutionContext().getRetry();
        this.runningTimeInMs = taskWrapper.getTaskExecutionContext().getRunningTimeInMs();
        this.maxRunningTimeInMilliseconds = taskWrapper.getTaskConfig().getMaxRunningTimeInMilliseconds();
        this.taskName = taskWrapper.getTaskExecutionContext().getTaskName();
        this.taskClass = taskWrapper.getTask().getClass();
    }

    public Class getTaskClass() {
        return taskClass;
    }

    public long getRunningTimeInMs() {
        return runningTimeInMs;
    }

    public int getRetry() {
        return retry;
    }

    public String getTaskName() {
        return taskName;
    }

    public String getTaskId() {
        return taskId;
    }

    public int getMaxRunningTimeInMilliseconds() {
        return maxRunningTimeInMilliseconds;
    }
}
