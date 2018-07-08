package com.whiletrue.clustertasks.tasks;

public class TaskExecutionContext {
    private int retry;
    private String clusterNodeId;
    private String taskId;
    private String taskName;


    public TaskExecutionContext(int retry, String clusterNodeId, String taskId, String taskName) {
        this.retry = retry;
        this.clusterNodeId = clusterNodeId;
        this.taskId = taskId;
        this.taskName = taskName;
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
}
