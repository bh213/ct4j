package com.whiletrue.clustertasks.factory;

import com.whiletrue.clustertasks.tasks.Task;
///
public interface TaskFactory {
    <TASK extends Task, INPUT>  Task<INPUT> createInstance(Class<TASK> taskClass) throws Exception;
    void addCustomTaskFactory(ClusterTasksCustomFactory events);
    void removeCustomTaskFactory(ClusterTasksCustomFactory events);
}
