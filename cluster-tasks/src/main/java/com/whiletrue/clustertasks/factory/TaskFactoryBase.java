package com.whiletrue.clustertasks.factory;

import com.whiletrue.clustertasks.tasks.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class TaskFactoryBase implements TaskFactory {
    private final List<ClusterTasksCustomFactory> customTaskFactories = new ArrayList<>();

    @Override
    public synchronized <TASK extends Task, INPUT> Task<INPUT> createInstance(Class<TASK> taskClass) throws Exception {
        for (ClusterTasksCustomFactory customTaskFactory: customTaskFactories) {
            final Task<INPUT> task = customTaskFactory.createInstance(taskClass);
            if (task != null) return task;
        }
        return null;
    }

    public synchronized int getCustomTaskFactoriesCount(){
        return customTaskFactories.size();
    }

    @Override
    synchronized public void addCustomTaskFactory(ClusterTasksCustomFactory customFactory) {
        customTaskFactories.add(Objects.requireNonNull(customFactory, "customFactory must not be null"));
    }

    @Override
    public synchronized void removeCustomTaskFactory(ClusterTasksCustomFactory customFactory) {
        customTaskFactories.remove(Objects.requireNonNull(customFactory, "customFactory must not be null"));
    }
}
