package com.whiletrue.ct4j.tasks;

import java.time.Instant;

public class TaskWrapper<INPUT> {
    private final Task<INPUT> task;
    private INPUT input;
    private TaskExecutionContext taskExecutionContext;
    private Instant lastUpdated;
    private final TaskConfig taskConfig;

    public TaskExecutionContext getTaskExecutionContext() {
        return taskExecutionContext;
    }

    public TaskWrapper(Task<INPUT> task, INPUT input, TaskExecutionContext taskExecutionContext, Instant lastUpdated, TaskConfig taskConfig) {
        this.task = task;
        this.input = input;
        this.taskExecutionContext = taskExecutionContext;
        this.lastUpdated = lastUpdated;
        this.taskConfig = taskConfig;
    }

    public Task<INPUT> getTask() {
        return task;
    }

    public INPUT getInput() {
        return input;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public TaskConfig getTaskConfig() {
        return taskConfig;
    }
}
