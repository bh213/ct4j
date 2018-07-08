package com.whiletrue.clustertasks.tasks;

public class ClusterTasksConfig {

    private int defaultPriority = 1000;
    private int maxNumberOfTasksPerNode = 200;
    private int maximumPollingTimeMilliseconds = 5000;
    private int minimumPollingTimeMilliseconds = 50;

    private int instanceCheckinTimeInMilliseconds = 500;
    private ResourceUsage availableResources;
    private int defaultRetries = 3;
    private int defaultRetryDelay = 1000;
    private float defaultRetryBackoffFactor = 1.0f;

    private boolean enableQueueTaskShortcut;  // TODO
    private boolean enableQueueTaskValidation; // TODO

    public int getMaxNumberOfTasksPerNode() {
        return maxNumberOfTasksPerNode;
    }

    public int getMaximumPollingTimeMilliseconds() {
        return maximumPollingTimeMilliseconds;
    }

    public int getInstanceCheckinTimeInMilliseconds() {
        return instanceCheckinTimeInMilliseconds;
    }

    public ResourceUsage getAvailableResources() {
        return availableResources;
    }

    public int getDefaultRetries() {
        return defaultRetries;
    }

    public int getDefaultRetryDelay() {
        return defaultRetryDelay;
    }

    public float getDefaultRetryBackoffFactor() {
        return defaultRetryBackoffFactor;
    }

    public int getMinimumPollingTimeMilliseconds() {
        return minimumPollingTimeMilliseconds;
    }

    public int getDefaultPriority() {
        return defaultPriority;
    }
}
