package com.whiletrue.clustertasks.inmemory;

import com.whiletrue.clustertasks.factory.TaskFactoryBase;
import com.whiletrue.clustertasks.tasks.Task;

public class DefaultConstructorTaskFactory extends TaskFactoryBase {

    @Override
    public <TASK extends Task> Task createInstance(Class<TASK> taskClass) throws Exception {
        final Task instance = super.createInstance(taskClass);
        if (instance == null) return taskClass.newInstance();
        else return instance;
    }

};
