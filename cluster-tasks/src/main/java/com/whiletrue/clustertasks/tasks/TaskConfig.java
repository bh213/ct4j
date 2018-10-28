package com.whiletrue.clustertasks.tasks;

public class TaskConfig {


    private final RetryPolicy retryPolicy;
    private final ResourceUsage resourceUsage;
    private int priority;

    private String taskName;

    private int maximumNumberOfConcurrentTasksOfThisType;
    private final int maxRunningTimeInMilliseconds;

    private TaskConfig(int maxRunningTimeInMilliseconds, RetryPolicy retryPolicy, ResourceUsage resourceUsage, int priority, int concurrentTasksOfThisType, String taskName) {
        this.maxRunningTimeInMilliseconds = maxRunningTimeInMilliseconds;
        this.retryPolicy = retryPolicy;
        this.resourceUsage = resourceUsage;
        this.priority = priority;
        this.maximumNumberOfConcurrentTasksOfThisType = concurrentTasksOfThisType;
        this.taskName = taskName;
    }

    public int getMaxRunningTimeInMilliseconds() {
        return maxRunningTimeInMilliseconds;
    }

    public RetryPolicy getRetryPolicy() {
        return retryPolicy;
    }

    public ResourceUsage getResourceUsage() {
        return resourceUsage;
    }

    public int getPriority() {
        return priority;
    }

    public String getTaskName() {
        return taskName;
    }

    public int getMaximumNumberOfConcurrentTasksOfThisType() {
        return maximumNumberOfConcurrentTasksOfThisType;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("TaskConfig{");
        sb.append("maxRunningTimeInMilliseconds=").append(maxRunningTimeInMilliseconds);
        sb.append(", retryPolicy=").append(retryPolicy);
        sb.append(", resourceUsage=").append(resourceUsage);
        sb.append(", priority=").append(priority);
        sb.append(", maximumNumberOfConcurrentTasksOfThisType=").append(maximumNumberOfConcurrentTasksOfThisType);
        sb.append('}');
        return sb.toString();
    }

    public static class TaskConfigBuilder {

        private final ClusterTasksConfig config;
        private final ClusterTask annotation;

        private int maxRunningTimeInMilliseconds = 60 * 1000;
        private int priority;
        private int maxRetries;
        private int retryDelay;
        private float retryBackoffFactor;
        private boolean tryToRunRetryOnDifferentNode = false; // TODO
        private float estimatedCpuCoresUsage = 1.0f;
        private float memoryUsageInMb = 1.0f; // TODO: too much?
        private int maximumNumberOfConcurrentTasksOfThisType = -1;
        private String taskName;

        public TaskConfigBuilder(ClusterTasksConfig config, ClusterTask annotation, Class taskClass) {
            this.taskName = taskClass.getName();
            this.config = config;
            this.annotation = annotation;

            this.priority = config.getDefaultPriority();
            this.maxRetries = config.getDefaultRetries();
            this.retryDelay = config.getDefaultRetryDelay();
            this.retryBackoffFactor = config.getDefaultRetryBackoffFactor();
            if (annotation != null) {

                if (annotation.defaultPriority() > 0) this.priority = annotation.defaultPriority();
                if (annotation.maxRetries() >= 0) this.maxRetries = annotation.maxRetries();
                if (annotation.retryDelay() >= 0) this.retryDelay = annotation.retryDelay();
                if (annotation.retryBackoffFactor() >= 0) this.retryBackoffFactor = annotation.retryBackoffFactor();
                if (annotation.name() != null) this.taskName = annotation.name();
            }
        }


        public TaskConfigBuilder setMaxRunningtime(int maxRunningTimeInMilliseconds) {
            this.maxRunningTimeInMilliseconds = maxRunningTimeInMilliseconds;
            return this;
        }


        public TaskConfigBuilder setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public TaskConfigBuilder setPriority(int priority) {
            this.priority = priority;
            return this;
        }

        public TaskConfigBuilder setRetryDelay(int retryDelay, int retryBackoffFactor) {
            this.retryDelay = retryDelay;
            this.retryBackoffFactor = retryBackoffFactor;
            return this;
        }

/*
        public TaskConfigBuilder tryToRunOnDifferentNodeOnRetry(boolean forceRunOnDifferentNode) {
            this.tryToRunRetryOnDifferentNode = forceRunOnDifferentNode;
            return this;
        }
*/

        public TaskConfigBuilder estimateResourceUsage(float estimatedCpuCoresUsage, float memoryUsageInMb) {
            this.estimatedCpuCoresUsage = estimatedCpuCoresUsage;
            this.memoryUsageInMb = memoryUsageInMb;
            return this;
        }

/*
        public TaskConfigBuilder setMaximumNumberOfConcurrentTasksOfThisType(int maxConcurrentTasksOfThisType) {
            this.maximumNumberOfConcurrentTasksOfThisType = maxConcurrentTasksOfThisType;
            return this;
        }
*/

        public TaskConfigBuilder setTaskName(String taskName) {
            this.taskName = taskName;
            return this;
        }


        public TaskConfig build() {

            if (maxRetries < 0) throw new IllegalArgumentException("maxRetries must be greater than 0");
            if (retryDelay < 0) throw new IllegalArgumentException("retryDelay must be greater than 0");
            if (priority < 0) throw new IllegalArgumentException("priority must be greater than 0");
            if (taskName  == null || taskName.trim().length() == 0) throw new IllegalArgumentException("TaskName must be non-empty");


            return new TaskConfig(maxRunningTimeInMilliseconds,
                    new RetryPolicy(maxRetries, retryDelay, retryBackoffFactor, tryToRunRetryOnDifferentNode),
                    new ResourceUsage(estimatedCpuCoresUsage, memoryUsageInMb),
                    priority,
                    maximumNumberOfConcurrentTasksOfThisType,
                    taskName);
        }
    }
}


