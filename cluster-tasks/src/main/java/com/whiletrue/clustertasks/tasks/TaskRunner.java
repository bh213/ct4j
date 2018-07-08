package com.whiletrue.clustertasks.tasks;

import com.whiletrue.clustertasks.scheduler.InternalTaskEvents;

public interface TaskRunner {
    <INPUT> void executeTask(TaskWrapper<INPUT> taskWrapper, InternalTaskEvents internalTaskEvents);

    int getFreeTasksSlots();

    ResourceUsage getCurrentResourcesAvailable();
}
