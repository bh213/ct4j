package com.whiletrue.ct4j.tasks;

import com.whiletrue.ct4j.instanceid.ClusterInstance;

import java.util.List;

public interface ClusterNodePersistence {
    void instanceInitialCheckIn(String uniqueRequestId);
    void instanceFinalCheckOut(String uniqueRequestId);
    List<ClusterInstance> instanceHeartbeat(List<ClusterInstance> previousInstances, String previousUniqueRequestId, String newUniqueRequestId);
    List<ClusterInstance> getClusterInstances();
}
