package com.whiletrue.ct4j.tasks;

import com.whiletrue.ct4j.factory.ClusterTasksCustomFactory;
import com.whiletrue.ct4j.instanceid.ClusterInstance;
import com.whiletrue.ct4j.scheduler.SchedulerCallbackListener;

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
