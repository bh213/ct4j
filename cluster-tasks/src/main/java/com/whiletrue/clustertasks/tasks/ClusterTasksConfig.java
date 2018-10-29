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
    private boolean schedulerFitAsManyTaskAsPossible = false;
    private boolean schedulerIgnoreResourcesForHighestPriorityTask = false;
    private boolean schedulerPollAfterTaskCompletion = true;
    private boolean schedulerAdaptivePollingRate = true;

    public boolean isEnableQueueTaskShortcut() {
        return enableQueueTaskShortcut;
    }

    public boolean isSchedulerFitAsManyTaskAsPossible() {
        return schedulerFitAsManyTaskAsPossible;
    }

    public boolean isSchedulerIgnoreResourcesForHighestPriorityTask() {
        return schedulerIgnoreResourcesForHighestPriorityTask;
    }

    public boolean isSchedulerPollAfterTaskCompletion() {
        return schedulerPollAfterTaskCompletion;
    }

    public boolean isSchedulerAdaptivePollingRate() {
        return schedulerAdaptivePollingRate;
    }

    public int getMaxNumberOfTasksPerNode() {
        return maxNumberOfTasksPerNode;
    }

    public int getMaximumPollingTimeMilliseconds() {
        return maximumPollingTimeMilliseconds;
    }

    public int getInstanceCheckinTimeInMilliseconds() {
        return instanceCheckinTimeInMilliseconds;
    }

    public ResourceUsage getConfiguredResources() {
        return availableResources;
    }  // TODO: use this

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


    public ClusterTasksConfig() {

        long allocatedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long presumableFreeMemory = Runtime.getRuntime().maxMemory() - allocatedMemory;

        this.availableResources = new ResourceUsage(Runtime.getRuntime().availableProcessors(), presumableFreeMemory / 1000000.0f, "custom resource 1", 100, "custom resource 2", 100);

    }
}
