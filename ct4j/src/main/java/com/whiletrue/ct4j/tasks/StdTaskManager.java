package com.whiletrue.ct4j.tasks;

import com.whiletrue.ct4j.factory.TaskFactory;
import com.whiletrue.ct4j.instanceid.ClusterInstance;
import com.whiletrue.ct4j.scheduler.ExecutionStats;
import com.whiletrue.ct4j.scheduler.Scheduler;
import com.whiletrue.ct4j.scheduler.TaskPerformanceStatsInterval;
import com.whiletrue.ct4j.scheduler.TaskPerformanceStatsSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class StdTaskManager implements TaskManager {

    private static Logger log = LoggerFactory.getLogger(StdTaskManager.class);
    private TaskPersistence taskPersistence;
    private TaskFactory taskFactory;
    private Scheduler scheduler;
    private TaskCallbacksListener callbacksListener;

    public StdTaskManager(TaskPersistence taskPersistence, TaskFactory taskFactory, Scheduler scheduler) {
        this.taskPersistence = taskPersistence;
        this.taskFactory = taskFactory;
        this.scheduler = scheduler;
    }

    @Override
    public <INPUT, TASK extends Task<INPUT>> String queueTask(Class<TASK> taskClass, INPUT input) throws Exception {
        Task<INPUT> instance = taskFactory.createInstance(taskClass);
        return taskPersistence.queueTask(instance, input);
    }

    @Override
    public <INPUT, TASK extends Task<INPUT>> String queueTask(Class<TASK> taskClass, INPUT input, int priority) throws Exception {
        Task<INPUT> instance = taskFactory.createInstance(taskClass);
        return taskPersistence.queueTask(instance, input, priority);
    }


    @Override
    public <INPUT, TASK extends Task<INPUT>> String queueTaskDelayed(Class<TASK> taskClass, INPUT input, long millisecondStartDelay) throws Exception {
        Task<INPUT> instance = taskFactory.createInstance(taskClass);
        return taskPersistence.queueTaskDelayed(instance, input, millisecondStartDelay);
    }

    @Override
    public <INPUT, TASK extends Task<INPUT>> String queueTaskDelayed(Class<TASK> taskClass, INPUT input, long millisecondStartDelay, int priority) throws Exception {
        Task<INPUT> instance = taskFactory.createInstance(taskClass);
        return taskPersistence.queueTaskDelayed(instance, input, millisecondStartDelay, priority);

    }

    @Override
    public <INPUT, TASK extends Task<INPUT>> String registerScheduledTask(Class<TASK> taskClass, INPUT input, int periodInMilliseconds, ScheduledTaskAction scheduledTaskAction) throws Exception {
        Task<INPUT> instance = taskFactory.createInstance(taskClass);
        return taskPersistence.registerScheduledTask(instance, input, periodInMilliseconds, scheduledTaskAction);
    }

    @Override
    public void stopScheduling() {
        scheduler.stopScheduling();
    }

    @Override
    public void startScheduling() {
        scheduler.startScheduling();
    }

    @Override
    public long countPendingTasks() {
        return taskPersistence.countPendingTasks();
    }

    @Override
    public TaskStatus getTaskStatus(String taskId) {
        return taskPersistence.getTaskStatus(taskId);
    }

    @Override
    public ResourceUsage getFreeResourcesEstimate(){
        return scheduler.getFreeResourcesEstimate();
    }

    @Override
    public TaskCallbacksListener getCallbacksListener() {
        return callbacksListener;
    }

    @Override
    public synchronized TaskCallbacksListener setCallbacksListener(TaskCallbacksListener callbacksListener) {
        log.info("Settings callback listener to {}", callbacksListener);

        TaskCallbacksListener oldCallback = this.callbacksListener;
        if (oldCallback != null) taskFactory.removeCustomTaskFactory(oldCallback);
        this.callbacksListener = callbacksListener;

        if (callbacksListener != null) {
            taskFactory.addCustomTaskFactory(callbacksListener);
            scheduler.setCallbackListener(callbacksListener);
        }
        return oldCallback;
    }


    @Override
    public List<ClusterInstance> getClusterInstances() {
        return scheduler.getClusterInstances();
    }

    @Override
    public TaskPerformanceStatsInterval getPerformanceInterval(TaskPerformanceStatsSnapshot start, TaskPerformanceStatsSnapshot end){
        return new TaskPerformanceStatsInterval(Objects.requireNonNull(start, "start must not be null"), Objects.requireNonNull(start, "end must not be null"));
    }

    @Override
    public Map<String, ExecutionStats> getPerformanceSnapshot(){
        return scheduler.getPerformanceSnapshot().toMap();
    }
}
