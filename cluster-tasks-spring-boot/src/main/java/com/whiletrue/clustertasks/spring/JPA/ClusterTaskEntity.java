package com.whiletrue.clustertasks.spring.JPA;


import com.whiletrue.clustertasks.tasks.TaskStatus;

import javax.persistence.*;
import java.util.Date;

@Entity()
@Table(name = "ct4j_tasks", indexes = @Index(name="ct4j_tasks_next_run_index", columnList = "nextRun,priority"))

public class ClusterTaskEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ct4j_tasks_id_seq")
    @SequenceGenerator(name = "ct4j_tasks_id_seq", sequenceName = "ct4j_tasks_id_seq", allocationSize = 50)
    private Long id;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(nullable = false, length = 256)
    private String taskClass;

    private String input;

    @Column(nullable = true, length = 256)
    private String inputClass;

    @Column(nullable = false)
    @Temporal(TemporalType.TIMESTAMP)

    private Date nextRun;

    @Column(nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastUpdate;


    @Column(nullable = true, length = 64)
    private String lockedByInstanceId;
    @Column(nullable = true)
    @Temporal(TemporalType.TIMESTAMP)
    private Date lockTime;
    @Column(nullable = true)
    private Integer retryCount;
    @Column(nullable = false)
    private int priority;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TaskStatus status;

    @Column(nullable = true)
    @Temporal(TemporalType.TIMESTAMP)
    private Date nextScheduledTime;

    @Column(nullable = true, length = 128)
    private String recurringSchedule;

    public Date getNextScheduledTime() {
        return nextScheduledTime;
    }

    public void setNextScheduledTime(Date nextScheduledTime) {
        this.nextScheduledTime = nextScheduledTime;
    }

    public String getRecurringSchedule() {
        return recurringSchedule;
    }

    public void setRecurringSchedule(String recurringSchedule) {
        this.recurringSchedule = recurringSchedule;
    }

    public ClusterTaskEntity() {
    }

    public Date getNextRun() {
        return nextRun;
    }

    public void setNextRun(Date nextRun) {
        this.nextRun = nextRun;
    }

    public Date getLockTime() {
        return lockTime;
    }

    public void setLockTime(Date lockTime) {
        this.lockTime = lockTime;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public String getLockedByInstanceId() {
        return lockedByInstanceId;
    }

    public void setLockedByInstanceId(String lockedByInstanceId) {
        this.lockedByInstanceId = lockedByInstanceId;
    }


    @Override
    public String toString() {
        return "ClusterTaskEntity{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", nextRun=" + nextRun +
                ", lockedByInstanceId='" + lockedByInstanceId + '\'' +
                ", lockTime=" + lockTime +
                '}';
    }

    public String getTaskClass() {
        return taskClass;
    }

    public void setTaskClass(String taskClass) {
        this.taskClass = taskClass;
    }

    public String getInputClass() {
        return inputClass;
    }

    public void setInputClass(String inputClass) {
        this.inputClass = inputClass;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }
}