package com.whiletrue.sample;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "benchmark")
public class BenchmarkConfigurationProperties {

    public enum BenchmarkMode {
        NODE,
        GENERATOR
    }

    private String testGetUrl;
    private BenchmarkMode mode;

    private int RESTTaskPerSecond = 3;
    private int CPUTasksPerSecond = 2;
    private int FailingTasksPerSecond = 2;
    private int ShortTasksPerSecond = 100;


    public int getShortTasksPerSecond() {
        return ShortTasksPerSecond;
    }

    public void setShortTasksPerSecond(int shortTasksPerSecond) {
        ShortTasksPerSecond = shortTasksPerSecond;
    }




    public int getRESTTaskPerSecond() {
        return RESTTaskPerSecond;
    }

    public void setRESTTaskPerSecond(int RESTTaskPerSecond) {
        this.RESTTaskPerSecond = RESTTaskPerSecond;
    }

    public int getCPUTasksPerSecond() {
        return CPUTasksPerSecond;
    }

    public void setCPUTasksPerSecond(int CPUTasksPerSecond) {
        this.CPUTasksPerSecond = CPUTasksPerSecond;
    }

    public int getFailingTasksPerSecond() {
        return FailingTasksPerSecond;
    }

    public void setFailingTasksPerSecond(int failingTasksPerSecond) {
        FailingTasksPerSecond = failingTasksPerSecond;
    }

    public String getTestGetUrl() {
        return testGetUrl;
    }

    public void setTestGetUrl(String testGetUrl) {
        this.testGetUrl = testGetUrl;
    }

    public BenchmarkMode getMode() {
        return mode;
    }

    public void setMode(BenchmarkMode mode) {
        this.mode = mode;
    }
}
