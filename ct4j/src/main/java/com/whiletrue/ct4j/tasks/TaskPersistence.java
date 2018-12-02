package com.whiletrue.ct4j.tasks;

import java.time.Instant;
import java.util.List;

public interface TaskPersistence {

    List<TaskWrapper<?>> pollForNextTasks(int maxTasks) throws Exception;
    List<TaskWrapper<?>> findClaimedTasks(List<TaskWrapper<?>> tasks) throws Exception;

    int tryClaimTasks(List<TaskWrapper<?>> tasks);

    <INPUT> String queueTask(Task<INPUT> task, INPUT input) throws Exception;
    <INPUT> String queueTask(Task<INPUT> task, INPUT input, int priority) throws Exception;
    <INPUT> String queueTaskDelayed(Task<INPUT> task, INPUT input, long startDelayInMilliseconds) throws Exception;
    <INPUT> String queueTaskDelayed(Task<INPUT> task, INPUT input, long startDelayInMilliseconds, int priority) throws Exception;
    <INPUT> String registerScheduledTask(Task<INPUT> instance, INPUT input, int periodInMilliseconds, ScheduledTaskAction scheduledTaskAction) throws Exception;




    void deleteTask(String id);
    boolean unlockAndChangeStatus(List<TaskWrapper<?>> tasks, TaskStatus status);
//    TODO: void setTaskRunning();
//    TODO: void setTaskCancelled();

    boolean unlockAndMarkForRetry(TaskWrapper<?> task, int retryCount, Instant nextRun);
    boolean unlockAndMarkForRetryAndSetScheduledNextRun(TaskWrapper<?> task, int retryCount, Instant nextRun, Instant nextScheduledRun);


    TaskWrapper<?> getTask(String taskId);

    TaskStatus getTaskStatus(String taskId);
    long countPendingTasks();


    /**
     *
     * @return ClusterNodePersistence instance if task is clustered (@see isClustered() returns true)
     */
    ClusterNodePersistence getClusterNodePersistence();

    /**
     *
     * @return true if this persistence supports cluster and false if it does not
     */
    boolean isClustered();

    /**
     *
     * @return true if this persistence class stores task data permanently or false if data is lost after node is terminated
     */
    boolean isPersistent();


}
