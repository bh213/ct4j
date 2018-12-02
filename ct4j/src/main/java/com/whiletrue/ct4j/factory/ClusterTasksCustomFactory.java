package com.whiletrue.ct4j.factory;

import com.whiletrue.ct4j.tasks.Task;

@FunctionalInterface
public interface ClusterTasksCustomFactory {
    Task createInstance(Class<? extends Task> taskClass);
}
