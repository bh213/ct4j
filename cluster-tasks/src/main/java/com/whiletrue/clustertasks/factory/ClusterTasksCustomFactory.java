package com.whiletrue.clustertasks.factory;

import com.whiletrue.clustertasks.tasks.Task;

@FunctionalInterface
public interface ClusterTasksCustomFactory {
    Task createInstance(Class<? extends Task> taskClass);
}
