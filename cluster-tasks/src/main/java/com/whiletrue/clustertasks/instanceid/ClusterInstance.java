package com.whiletrue.clustertasks.instanceid;

import com.whiletrue.clustertasks.tasks.ResourceUsage;

import java.time.Instant;

public class ClusterInstance {

    private String instanceId;
    private Instant lastCheckIn;
    private int checkInIntervalMilliseconds;
    private ClusterInstanceStatus status;
    private Instant checkStatusRequest;
    private ResourceUsage resourcesAvailable;


    public ClusterInstance(ClusterInstance original) {
        this.instanceId = original.instanceId;
        this.lastCheckIn = original.lastCheckIn;
        this.checkInIntervalMilliseconds = original.checkInIntervalMilliseconds;
        this.status = original.status;
        this.checkStatusRequest = original.checkStatusRequest;
        this.resourcesAvailable = original.resourcesAvailable;
    }



    public ClusterInstance(String instanceId, Instant lastCheckIn, int checkInIntervalMilliseconds, ClusterInstanceStatus status, Instant checkStatusRequest, ResourceUsage resourcesAvailable) {
        this.instanceId = instanceId;
        this.lastCheckIn = lastCheckIn;
        this.checkInIntervalMilliseconds = checkInIntervalMilliseconds;
        this.status = status;
        this.checkStatusRequest = checkStatusRequest;
        this.resourcesAvailable = resourcesAvailable;
    }

    public Instant getCheckStatusRequest() {
        return checkStatusRequest;
    }

    public ResourceUsage getResourcesAvailable() {
        return resourcesAvailable;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ClusterInstance{");
        sb.append("instanceId='").append(instanceId).append('\'');
        sb.append(", lastCheckIn=").append(lastCheckIn);
        sb.append(", checkInIntervalMilliseconds=").append(checkInIntervalMilliseconds);
        sb.append(", status=").append(status);
        sb.append(", checkStatusRequest=").append(checkStatusRequest);
        sb.append(", resourcesAvailable=").append(resourcesAvailable);
        sb.append('}');
        return sb.toString();
    }

    public ClusterInstanceStatus getStatus() {
        return status;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public Instant getLastCheckIn() {
        return lastCheckIn;
    }

    public int getCheckInIntervalMilliseconds() {
        return checkInIntervalMilliseconds;
    }

}
