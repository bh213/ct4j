package com.whiletrue.clustertasks.tasks;

import com.whiletrue.clustertasks.factory.ClusterTasksCustomFactory;
import com.whiletrue.clustertasks.factory.TaskFactory;
import com.whiletrue.clustertasks.scheduler.ExecutionStats;
import com.whiletrue.clustertasks.scheduler.Scheduler;
import com.whiletrue.clustertasks.scheduler.TaskPerformanceStatsInterval;
import com.whiletrue.clustertasks.scheduler.TaskPerformanceStatsSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

public class StdTaskManager implements TaskManager {

    private static Logger log = LoggerFactory.getLogger(StdTaskManager.class);
    private TaskPersistence taskPersistence;
    private TaskFactory taskFactory;
    private Scheduler scheduler;
    private TaskPerformanceStatsSnapshot lastPerformanceSnapshot = null;

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
    public void stopScheduling() {
        scheduler.stopScheduling();
    }

    @Override
    public void startScheduling() {
        scheduler.startScheduling();
    }


    @Override
    public ResourceUsage getFreeResourcesEstimate(){
        return scheduler.getFreeResourcesEstimate();
    }

    @Override
    public void addCustomTaskFactory(ClusterTasksCustomFactory customTaskFactory) {
        taskFactory.addCustomTaskFactory(customTaskFactory);
    }

    @Override
    public void remoteCustomTaskFactory(ClusterTasksCustomFactory customTaskFactory) {
        taskFactory.removeCustomTaskFactory(customTaskFactory);
    }



    @Override
    public TaskPerformanceStatsInterval getPerformanceInterval(TaskPerformanceStatsSnapshot start, TaskPerformanceStatsSnapshot end){
        return new TaskPerformanceStatsInterval(Objects.requireNonNull(start, "start must not be null"), Objects.requireNonNull(start, "end must not be null"));
    }

    @Override
    public Map<String, ExecutionStats> getPerformanceSnapshot(){

        return scheduler.getPerformanceSnapshot().toMap();
        /*String retVal = "";
        if (lastPerformanceSnapshot != null) {

            TaskPerformanceStatsInterval interval = new TaskPerformanceStatsInterval(lastPerformanceSnapshot, currentPerformanceSnapshot);
            retVal = interval.toTableString();
        }
        lastPerformanceSnapshot = currentPerformanceSnapshot;
        return retVal;*/
    }
}
