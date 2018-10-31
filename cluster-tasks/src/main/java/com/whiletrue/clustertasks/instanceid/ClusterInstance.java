package com.whiletrue.clustertasks.instanceid;

import java.time.Instant;

public class ClusterInstance {

    private String instanceId;
    private Instant lastCheckIn;
    private int checkInIntervalMilliseconds;


    public ClusterInstance(String instanceId, Instant lastCheckIn, int checkInIntervalMilliseconds) {
        this.instanceId = instanceId;
        this.lastCheckIn = lastCheckIn;
        this.checkInIntervalMilliseconds = checkInIntervalMilliseconds;
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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ClusterInstance{");
        sb.append("instanceId='").append(instanceId).append('\'');
        sb.append(", lastCheckIn=").append(lastCheckIn);
        sb.append(", checkInIntervalMilliseconds=").append(checkInIntervalMilliseconds);
        sb.append('}');
        return sb.toString();
    }
}
