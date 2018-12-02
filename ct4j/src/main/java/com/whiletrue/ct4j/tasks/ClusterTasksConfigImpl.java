package com.whiletrue.ct4j.tasks;

public class ClusterTasksConfigImpl implements ClusterTasksConfig {

    public static final int MAXIMUM_NUMBER_OF_TASKS_PER_NODE = 200;
    public static final int MAXIMUM_POLLING_TIME_MILLISECONDS = 5000;
    public static final int MINIMUM_POLLING_TIME_MILLISECONDS = 50;
    public static final int DEFAULT_RETRIES = 3;
    public static final int DEFAULT_RETRY_DELAY = 1000;
    public static final float DEFAULT_RETRY_BACKOFF_FACTOR = 1.0f;
    public static final int CLUSTER_INSTANCE_CHECKIN_TIME_IN_MILLISECONDS = 5000;
    public static final int DEFAULT_PRIORITY = 1000;
    public static final int CHECK_IN_FAILURE_INTERVAL_MULTIPLIER = 3;

    private int defaultPriority = DEFAULT_PRIORITY;
    private int maxNumberOfTasksPerNode = MAXIMUM_NUMBER_OF_TASKS_PER_NODE;
    private int maximumPollingTimeMilliseconds = MAXIMUM_POLLING_TIME_MILLISECONDS;
    private int minimumPollingTimeMilliseconds = MINIMUM_POLLING_TIME_MILLISECONDS;

    private int instanceCheckinTimeInMilliseconds = CLUSTER_INSTANCE_CHECKIN_TIME_IN_MILLISECONDS;
    private ResourceUsage availableResources;
    private int defaultRetries = DEFAULT_RETRIES;
    private int defaultRetryDelay = DEFAULT_RETRY_DELAY;
    private float defaultRetryBackoffFactor = DEFAULT_RETRY_BACKOFF_FACTOR;

    private boolean enableQueueTaskShortcut;  // TODO
    private boolean schedulerFitAsManyTaskAsPossible = false;
    private boolean schedulerIgnoreResourcesForHighestPriorityTask = false;
    private boolean schedulerPollAfterTaskCompletion = true;
    private boolean schedulerAdaptivePollingRate = true;
    private int checkInFailureIntervalMultiplier = CHECK_IN_FAILURE_INTERVAL_MULTIPLIER;

    public ClusterTasksConfigImpl() {

        long allocatedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long presumableFreeMemory = Runtime.getRuntime().maxMemory() - allocatedMemory;

        this.availableResources = new ResourceUsage(Runtime.getRuntime().availableProcessors(), presumableFreeMemory / 1000000.0f, "resource 1", 100, "resource 2", 100);

    }

    public void setAvailableResources(ResourceUsage availableResources) {
        this.availableResources = availableResources;
    }

    @Override
    public boolean isEnableQueueTaskShortcut() {
        return enableQueueTaskShortcut;
    }

    public void setEnableQueueTaskShortcut(boolean enableQueueTaskShortcut) {
        this.enableQueueTaskShortcut = enableQueueTaskShortcut;
    }

    @Override
    public boolean isSchedulerFitAsManyTaskAsPossible() {
        return schedulerFitAsManyTaskAsPossible;
    }

    public void setSchedulerFitAsManyTaskAsPossible(boolean schedulerFitAsManyTaskAsPossible) {
        this.schedulerFitAsManyTaskAsPossible = schedulerFitAsManyTaskAsPossible;
    }

    @Override
    public boolean isSchedulerIgnoreResourcesForHighestPriorityTask() {
        return schedulerIgnoreResourcesForHighestPriorityTask;
    }

    public void setSchedulerIgnoreResourcesForHighestPriorityTask(boolean schedulerIgnoreResourcesForHighestPriorityTask) {
        this.schedulerIgnoreResourcesForHighestPriorityTask = schedulerIgnoreResourcesForHighestPriorityTask;
    }

    @Override
    public boolean isSchedulerPollAfterTaskCompletion() {
        return schedulerPollAfterTaskCompletion;
    }

    public void setSchedulerPollAfterTaskCompletion(boolean schedulerPollAfterTaskCompletion) {
        this.schedulerPollAfterTaskCompletion = schedulerPollAfterTaskCompletion;
    }

    @Override
    public boolean isSchedulerAdaptivePollingRate() {
        return schedulerAdaptivePollingRate;
    }

    public void setSchedulerAdaptivePollingRate(boolean schedulerAdaptivePollingRate) {
        this.schedulerAdaptivePollingRate = schedulerAdaptivePollingRate;
    }

    @Override
    public int getMaxNumberOfTasksPerNode() {
        return maxNumberOfTasksPerNode;
    }

    public void setMaxNumberOfTasksPerNode(int maxNumberOfTasksPerNode) {
        this.maxNumberOfTasksPerNode = maxNumberOfTasksPerNode;
    }

    @Override
    public int getMaximumPollingTimeMilliseconds() {
        return maximumPollingTimeMilliseconds;
    }

    public void setMaximumPollingTimeMilliseconds(int maximumPollingTimeMilliseconds) {
        this.maximumPollingTimeMilliseconds = maximumPollingTimeMilliseconds;
    }

    @Override
    public int getInstanceCheckinTimeInMilliseconds() {
        return instanceCheckinTimeInMilliseconds;
    }

    public void setInstanceCheckinTimeInMilliseconds(int instanceCheckinTimeInMilliseconds) {
        this.instanceCheckinTimeInMilliseconds = instanceCheckinTimeInMilliseconds;
    }

    @Override
    public ResourceUsage getConfiguredResources() {
        return availableResources;
    }  // TODO: use this

    @Override
    public int getDefaultRetries() {
        return defaultRetries;
    }

    public void setDefaultRetries(int defaultRetries) {
        this.defaultRetries = defaultRetries;
    }

    @Override
    public int getDefaultRetryDelay() {
        return defaultRetryDelay;
    }

    public void setDefaultRetryDelay(int defaultRetryDelay) {
        this.defaultRetryDelay = defaultRetryDelay;
    }

    @Override
    public float getDefaultRetryBackoffFactor() {
        return defaultRetryBackoffFactor;
    }

    public void setDefaultRetryBackoffFactor(float defaultRetryBackoffFactor) {
        this.defaultRetryBackoffFactor = defaultRetryBackoffFactor;
    }

    @Override
    public int getMinimumPollingTimeMilliseconds() {
        return minimumPollingTimeMilliseconds;
    }

    public void setMinimumPollingTimeMilliseconds(int minimumPollingTimeMilliseconds) {
        this.minimumPollingTimeMilliseconds = minimumPollingTimeMilliseconds;
    }

    @Override
    public int getDefaultPriority() {
        return defaultPriority;
    }

    @Override
    public int getCheckInFailureIntervalMultiplier() {
        return checkInFailureIntervalMultiplier;
    }

    public void setCheckInFailureIntervalMultiplier(int checkInFailureIntervalMultiplier) {
        this.checkInFailureIntervalMultiplier = checkInFailureIntervalMultiplier;
    }

    public void setDefaultPriority(int defaultPriority) {
        this.defaultPriority = defaultPriority;
    }
}
