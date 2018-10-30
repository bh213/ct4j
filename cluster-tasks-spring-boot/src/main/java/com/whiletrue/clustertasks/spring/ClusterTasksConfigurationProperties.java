package com.whiletrue.clustertasks.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "ct4j", ignoreUnknownFields = false)
public class ClusterTasksConfigurationProperties {

    public static class Scheduler {
        boolean fitAsManyTaskAsPossible = true;
        boolean pollAfterTaskCompletion = true;
        Integer maximumPollingTimeMilliseconds = 5000;
        Integer minimumPollingTimeMilliseconds = 50;
        Integer maxNumberOfTasksPerNode = 200;
    }

    public static class Tasks {
        Integer defaultRetries = 3;
        Integer defaultRetryDelay = 1000;
        Float defaultRetryBackoffFactor = 1.5f;
        Float defaultPriority = 1000f;
    }

// TODO: resource usage

    private Scheduler scheduler = new Scheduler();
    private Tasks tasks = new Tasks();
    private String taskFactory = "spring";
    private String persistence = "memory";
    private String timeProvider = "local";



    public String getTaskFactory() {
        return taskFactory;
    }

    public String getPersistence() {
        return persistence;
    }

    public String getTimeProvider() {
        return timeProvider;
    }

    public void setTaskFactory(String taskFactory) {
        this.taskFactory = taskFactory;
    }

    public void setPersistence(String persistence) {
        this.persistence = persistence;
    }

    public void setTimeProvider(String timeProvider) {
        this.timeProvider = timeProvider;
    }
}
