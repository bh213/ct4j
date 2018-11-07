package com.whiletrue.clustertasks.tasks;

import com.whiletrue.clustertasks.factory.ClusterTasksCustomFactory;
import com.whiletrue.clustertasks.instanceid.ClusterInstance;
import com.whiletrue.clustertasks.scheduler.SchedulerCallbackListener;

public interface TaskCallbacksListener extends ClusterTasksCustomFactory, SchedulerCallbackListener {
    @Override
    default void taskOverdue(BasicTaskInfo task) {

    }

    @Override
    default void taskCompleted(BasicTaskInfo task) {

    }

    @Override
    default void taskFailed(BasicTaskInfo task) {

    }

    @Override
    default void taskCannotBeScheduled(BasicTaskInfo task) {

    }

    @Override
    default Task createInstance(Class<? extends Task> taskClass) {
        return null;
    }


    @Override
    default void clusterNodeStarted(ClusterInstance clusterInstance){}

    @Override
    default void clusterNodeStopped(ClusterInstance clusterInstance) {}
}
