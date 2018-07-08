package com.whiletrue.sample.jpa;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity()
@Table(name="test_task_table")
public class TestTaskTable {
    @Id
    private Long id;

    private long expectedRetries;

    @Column(nullable = false, length = 64)
    private String instanceId;
    private Boolean done;
    private Boolean retry1;
    private Boolean retry2;
    private Boolean retry3;
    private String error;
    private String log;


    public TestTaskTable() {
    }

    public TestTaskTable(Long id, long expectedRetries) {
        this.id = id;
        this.expectedRetries = expectedRetries;
        this.done = false;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public Boolean getDone() {
        return done;
    }

    public void setDone(Boolean done) {
        this.done = done;
    }

    public Boolean getRetry1() {
        return retry1;
    }

    public void setRetry1(Boolean retry1) {
        this.retry1 = retry1;
    }

    public Boolean getRetry2() {
        return retry2;
    }

    public void setRetry2(Boolean retry2) {
        this.retry2 = retry2;
    }

    public Boolean getRetry3() {
        return retry3;
    }

    public void setRetry3(Boolean retry3) {
        this.retry3 = retry3;
    }

    public long getExpectedRetries() {
        return expectedRetries;
    }

    public void setExpectedRetries(long expectedRetries) {
        this.expectedRetries = expectedRetries;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getLog() {
        return log;
    }

    public void setLog(String log) {
        this.log = log;
    }
}