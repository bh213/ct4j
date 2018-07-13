package com.whiletrue.clustertasks.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "ct4j", ignoreUnknownFields = false)
public class ClusterTasksConfigurationProperties {

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
}
