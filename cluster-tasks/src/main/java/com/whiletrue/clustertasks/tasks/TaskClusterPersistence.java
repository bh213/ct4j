package com.whiletrue.clustertasks.tasks;

import com.whiletrue.clustertasks.instanceid.ClusterInstance;

import java.util.List;

public interface TaskClusterPersistence {
    void instanceCheckIn();
    List<ClusterInstance> getClusterInstances();
}
