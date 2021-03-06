package com.whiletrue.ct4j.tasks;

import com.whiletrue.ct4j.scheduler.InternalTaskEvents;

import java.util.List;
import java.util.concurrent.Future;

public interface TaskRunner {
    <INPUT> Future<TaskStatus> executeTask(TaskWrapper<INPUT> taskWrapper, InternalTaskEvents internalTaskEvents);

    /**
     * @return Returns how many free slots for tasks this runner still has. This is the maximum number of tasks that
     * this runner can still execute
     */
    int getFreeTasksSlots();

    /**
     *  @return list of tasks overdue (running time longer than configured max running time)
     */
    List<BasicTaskInfo> getOverdueTasks();

    /**
     * @return currently running tasks
     */
    List<BasicTaskInfo> getRunningTasks();

    /**
     * @return returns resource currently available to TaskRunner (estimated)
     */
    ResourceUsage getCurrentResourcesAvailable();
}
