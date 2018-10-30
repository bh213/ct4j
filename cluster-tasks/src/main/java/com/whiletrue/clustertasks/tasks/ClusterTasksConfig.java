package com.whiletrue.clustertasks.tasks;

public interface ClusterTasksConfig {
    boolean isEnableQueueTaskShortcut();

    boolean isSchedulerFitAsManyTaskAsPossible();

    boolean isSchedulerIgnoreResourcesForHighestPriorityTask();

    boolean isSchedulerPollAfterTaskCompletion();

    boolean isSchedulerAdaptivePollingRate();

    int getMaxNumberOfTasksPerNode();

    int getMaximumPollingTimeMilliseconds();

    int getMinimumPollingTimeMilliseconds();

    int getInstanceCheckinTimeInMilliseconds();

    ResourceUsage getConfiguredResources();


    int getDefaultRetries();

    int getDefaultRetryDelay();

    float getDefaultRetryBackoffFactor();



    int getDefaultPriority();
}
