package com.whiletrue.clustertasks.JPA;

import com.whiletrue.clustertasks.tasks.TaskStatus;

import javax.persistence.*;
import java.time.Instant;
import java.util.Date;

@Entity()
@Table(name = "cluster_tasks_instances")
public class ClusterInstanceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String instanceId;

    @Temporal(TemporalType.TIMESTAMP)
    private Date lastCheckIn;

    private int checkInIntervalMilliseconds;


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

    public Date getLastCheckIn() {
        return lastCheckIn;
    }

    public void setLastCheckIn(Date lastCheckIn) {
        this.lastCheckIn = lastCheckIn;
    }

    public int getCheckInIntervalMilliseconds() {
        return checkInIntervalMilliseconds;
    }

    public void setCheckInIntervalMilliseconds(int checkInIntervalMilliseconds) {
        this.checkInIntervalMilliseconds = checkInIntervalMilliseconds;
    }
}
