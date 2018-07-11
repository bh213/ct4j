package com.whiletrue.clustertasks.tasks;

import com.whiletrue.clustertasks.factory.ClusterTasksCustomFactory;

public interface TaskManager {
    <INPUT, TASK extends Task<INPUT>> String queueTask(Class<TASK> taskClass, INPUT input) throws Exception;
    <INPUT, TASK extends Task<INPUT>> String queueTask(Class<TASK> taskClass, INPUT input, int priority) throws Exception;

    <INPUT, TASK extends Task<INPUT>> String queueTaskDelayed(Class<TASK> taskClass, INPUT input, long millisecondStartDelay) throws Exception;
    <INPUT, TASK extends Task<INPUT>> String queueTaskDelayed(Class<TASK> taskClass, INPUT input, long millisecondStartDelay, int priority) throws Exception;

    void stopScheduling();

    void startScheduling();

    ResourceUsage getFreeResourcesEstimate();

    String getStats();

    void addEventListener(ClusterTasksCustomFactory customTaskFactory);
    void removeEventListener(ClusterTasksCustomFactory customTaskFactory);
}

