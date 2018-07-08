package com.whiletrue.clustertasks.tasks;

import com.whiletrue.clustertasks.factory.ClusterTasksCustomFactory;
import com.whiletrue.clustertasks.factory.TaskFactory;
import com.whiletrue.clustertasks.scheduler.Scheduler;
import com.whiletrue.clustertasks.scheduler.TaskPerformanceStatsInterval;
import com.whiletrue.clustertasks.scheduler.TaskPerformanceStatsSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public void addEventListener(ClusterTasksCustomFactory customTaskFactory) {
        taskFactory.addCustomTaskFactory(customTaskFactory);
    }

    @Override
    public void removeEventListener(ClusterTasksCustomFactory customTaskFactory) {
        taskFactory.removeCustomTaskFactory(customTaskFactory);
    }

    // TODO: remove/change
    @Override
    public String getStats(){

        final TaskPerformanceStatsSnapshot currentPerformanceSnapshot = scheduler.getPerformanceSnapshot();
        String retVal = "";
        if (lastPerformanceSnapshot != null) {

            TaskPerformanceStatsInterval interval = new TaskPerformanceStatsInterval(lastPerformanceSnapshot, currentPerformanceSnapshot);
            retVal = interval.toTableString();
        }
        lastPerformanceSnapshot = currentPerformanceSnapshot;
        return retVal;
    }
}
