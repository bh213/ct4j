package com.whiletrue.clustertasks.tasks;

import com.whiletrue.clustertasks.instanceid.ClusterInstance;
import com.whiletrue.clustertasks.scheduler.ExecutionStats;
import com.whiletrue.clustertasks.scheduler.TaskPerformanceStatsInterval;
import com.whiletrue.clustertasks.scheduler.TaskPerformanceStatsSnapshot;

import java.util.List;
import java.util.Map;

public interface TaskManager {
    /**
     * Queues new task to be executed by ct4j
     * @param taskClass task class to be executed
     * @param input input for task
     * @param <INPUT> type of input
     * @param <TASK> task class
     * @return id of the task
     * @throws Exception
     */
    <INPUT, TASK extends Task<INPUT>> String queueTask(Class<TASK> taskClass, INPUT input) throws Exception;

    /**
     * Queues new task to be executed by ct4j
     * @param taskClass task class to be executed
     * @param input input for task
     * @param priority overrides priority for the task
     * @param <INPUT> type of input
     * @param <TASK> task class
     * @return id of the task
     * @throws Exception
     */
    <INPUT, TASK extends Task<INPUT>> String queueTask(Class<TASK> taskClass, INPUT input, int priority) throws Exception;

    /**
     * Queues new task to be executed by ct4j with after being delayed by at least {@code millisecondStartDelay}
     * @param taskClass task class to be executed
     * @param input input for task
     * @param millisecondStartDelay delay before queued task is going to be considered for execution
     * @param <INPUT> type of input
     * @param <TASK> task class
     * @return id of the task
     * @throws Exception
     */

    <INPUT, TASK extends Task<INPUT>> String queueTaskDelayed(Class<TASK> taskClass, INPUT input, long millisecondStartDelay) throws Exception;

    /**
     * Queues new task to be executed by ct4j with delay specified by {@code millisecondStartDelay} and custom {@code priority}
     * @param taskClass task class to be executed
     * @param input input for task
     * @param millisecondStartDelay delay before queued task is going to be considered for execution
     * @param priority overrides priority for the task
     * @param <INPUT> type of input
     * @param <TASK> task class
     * @return id of the task
     * @throws Exception
     */

    <INPUT, TASK extends Task<INPUT>> String queueTaskDelayed(Class<TASK> taskClass, INPUT input, long millisecondStartDelay, int priority) throws Exception;

    /**
     * Stops task scheduler - new tasks will no longer run on this node. Scheduler is stopped by default.
     */
    void stopScheduling();
    /**
     * Starts task scheduler - tasks will run on this node. Scheduler is stopped by default.
     */

    void startScheduling();

    /**
     * @return number of tasks in store (e.g. database) that are not being executed. Basically queue of tasks.
     */
    long countPendingTasks();

    /**
     *
     * @param taskId
     * @return Task status or null if not found or available
     */
    TaskStatus getTaskStatus(String taskId);

    /**
     * Estimates unused compute resources for this node. @see {@link ResourceUsage}
     * @return estimate of resources available to this node
     */
    ResourceUsage getFreeResourcesEstimate();

    TaskCallbacksListener getCallbacksListener();

    TaskCallbacksListener setCallbacksListener(TaskCallbacksListener callbacksListener);


    List<ClusterInstance> getClusterInstances();

    /**
     *
     * @param start starting performance interval
     * @param end ending performance interval
     * @return task performance statistics for given interval
     */
    TaskPerformanceStatsInterval getPerformanceInterval(TaskPerformanceStatsSnapshot start, TaskPerformanceStatsSnapshot end);

    /**
     *
     * @return current task performance snapshot. To be used with @see getPerformanceInterval to get number of processed/successful/failed tasks in a given interval.
     */
    Map<String, ExecutionStats> getPerformanceSnapshot();
}

