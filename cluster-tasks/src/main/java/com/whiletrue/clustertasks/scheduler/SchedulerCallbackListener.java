package com.whiletrue.clustertasks.scheduler;

import com.whiletrue.clustertasks.instanceid.ClusterInstance;
import com.whiletrue.clustertasks.tasks.BasicTaskInfo;

public interface SchedulerCallbackListener {
    void taskOverdue(BasicTaskInfo task);

    void taskCompleted(BasicTaskInfo task);

    void taskFailed(BasicTaskInfo task);

    void taskCannotBeScheduled(BasicTaskInfo task);

    void clusterNodeStarted(ClusterInstance clusterInstance);

    void clusterNodeStopped(ClusterInstance clusterInstance);

}
