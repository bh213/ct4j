package com.whiletrue.ct4j.scheduler;

import com.whiletrue.ct4j.instanceid.ClusterInstance;
import com.whiletrue.ct4j.tasks.BasicTaskInfo;

public interface SchedulerCallbackListener {
    void taskOverdue(BasicTaskInfo task);

    void taskCompleted(BasicTaskInfo task);

    void taskFailed(BasicTaskInfo task);

    void taskCannotBeScheduled(BasicTaskInfo task);

    void clusterNodeStarted(ClusterInstance clusterInstance);

    void clusterNodeStopped(ClusterInstance clusterInstance);

}
