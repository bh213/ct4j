package com.whiletrue.ct4j.factory;

import com.whiletrue.ct4j.tasks.Task;
///
public interface TaskFactory {
    <TASK extends Task, INPUT>  Task<INPUT> createInstance(Class<TASK> taskClass) throws Exception;
    void addCustomTaskFactory(ClusterTasksCustomFactory events);
    void removeCustomTaskFactory(ClusterTasksCustomFactory events);
}
