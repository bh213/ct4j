package com.whiletrue.clustertasks.spring.JPA;

import javax.persistence.*;
import java.util.Date;

@Entity()
@Table(name = "ct4j_instances")

public class ClusterInstanceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ct4j_instances_seq")
    @SequenceGenerator(name = "ct4j_instances_seq", sequenceName = "ct4j_instances_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false, length = 64)
    private String instanceId;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ClusterInstanceEntity{");
        sb.append("id=").append(id);
        sb.append(", instanceId='").append(instanceId).append('\'');
        sb.append(", lastCheckIn=").append(lastCheckIn);
        sb.append(", checkInIntervalMilliseconds=").append(checkInIntervalMilliseconds);
        sb.append('}');
        return sb.toString();
    }
}
