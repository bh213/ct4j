package com.whiletrue.clustertasks.spring.JPA;

import com.whiletrue.clustertasks.instanceid.ClusterInstanceStatus;

import javax.persistence.*;
import java.time.Instant;
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

    @Column(nullable = false, length = 64)
    private String uniqueRequestId;

    @Column(nullable = false)
    private boolean taskRefreshRequested;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ClusterInstanceStatus status;
    @Column(nullable = true)
    private Instant checkStatusRequest;

    @Column(nullable = false)
    private double availableResourcesCpuCoreUsage;
    @Column(nullable = false)
    private double availableResourcesMaximumMemoryUsageInMb;
    @Column(nullable = false)
    private double availableResourcesCustomResource1;
    @Column(nullable = false)
    private double availableResourcesCustomResource2;

    public ClusterInstanceStatus getStatus() {
        return status;
    }

    public void setStatus(ClusterInstanceStatus status) {
        this.status = status;
    }

    public Instant getCheckStatusRequest() {
        return checkStatusRequest;
    }

    public void setCheckStatusRequest(Instant checkStatusRequest) {
        this.checkStatusRequest = checkStatusRequest;
    }

    public double getAvailableResourcesCpuCoreUsage() {
        return availableResourcesCpuCoreUsage;
    }

    public void setAvailableResourcesCpuCoreUsage(double availableResourcesCpuCoreUsage) {
        this.availableResourcesCpuCoreUsage = availableResourcesCpuCoreUsage;
    }

    public double getAvailableResourcesMaximumMemoryUsageInMb() {
        return availableResourcesMaximumMemoryUsageInMb;
    }

    public void setAvailableResourcesMaximumMemoryUsageInMb(double availableResourcesMaximumMemoryUsageInMb) {
        this.availableResourcesMaximumMemoryUsageInMb = availableResourcesMaximumMemoryUsageInMb;
    }

    public double getAvailableResourcesCustomResource1() {
        return availableResourcesCustomResource1;
    }

    public void setAvailableResourcesCustomResource1(double availableResourcesCustomResource1) {
        this.availableResourcesCustomResource1 = availableResourcesCustomResource1;
    }

    public double getAvailableResourcesCustomResource2() {
        return availableResourcesCustomResource2;
    }

    public void setAvailableResourcesCustomResource2(double availableResourcesCustomResource2) {
        this.availableResourcesCustomResource2 = availableResourcesCustomResource2;
    }

    public String getUniqueRequestId() {
        return uniqueRequestId;
    }

    public void setUniqueRequestId(String uniqueRequestId) {
        this.uniqueRequestId = uniqueRequestId;
    }

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

    public boolean isTaskRefreshRequested() {
        return taskRefreshRequested;
    }

    public void setTaskRefreshRequested(boolean taskRefreshRequested) {
        this.taskRefreshRequested = taskRefreshRequested;
    }
}
