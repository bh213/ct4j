package com.whiletrue.ct4j.inmemory;

import com.whiletrue.ct4j.factory.TaskFactoryBase;
import com.whiletrue.ct4j.tasks.Task;

import java.lang.reflect.Constructor;

public class DefaultConstructorTaskFactory extends TaskFactoryBase {

    @Override
    public <TASK extends Task, INPUT> Task<INPUT> createInstance(Class<TASK> taskClass) throws Exception {
        final Task<INPUT> instance = super.createInstance(taskClass);
        if (instance == null) {
            final Constructor<TASK> constructor = taskClass.getConstructor();
            if (constructor == null) throw new NoSuchMethodException("Could not find default constructor");
            return constructor.newInstance();
        }
        else return instance;
    }
};
