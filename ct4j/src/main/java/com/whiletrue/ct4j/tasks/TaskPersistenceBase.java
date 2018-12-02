package com.whiletrue.ct4j.tasks;

import com.whiletrue.ct4j.tasks.recurring.RecurringSchedule;
import com.whiletrue.ct4j.tasks.recurring.RecurringScheduleStrategyFixed;
import com.whiletrue.ct4j.timeprovider.TimeProvider;
import org.slf4j.Logger;

import java.util.List;
import java.util.Objects;

public abstract  class TaskPersistenceBase implements TaskPersistence {

    public interface RecurringTaskEntity {
        String getTaskId();

    }

    protected Logger log;
    protected TimeProvider timeProvider;
    protected ClusterTasksConfig clusterTasksConfig;

    public TaskPersistenceBase(Logger log, TimeProvider timeProvider, ClusterTasksConfig clusterTasksConfig) {
        this.log = log;
        this.timeProvider = timeProvider;
        this.clusterTasksConfig = clusterTasksConfig;
    }

    public <INPUT> TaskConfig getTaskConfig(Task<INPUT> task, ClusterTasksConfig clusterTasksConfig) {
        final Class<? extends Task> taskClass = Objects.requireNonNull(task).getClass();
        final ClusterTask clusterTaskAnnotation = Utils.getClusterTaskAnnotation(taskClass);
        final TaskConfig.TaskConfigBuilder builder = new TaskConfig.TaskConfigBuilder(clusterTasksConfig, clusterTaskAnnotation, taskClass);
        TaskConfig taskConfig = task.configureTask(builder);
        if (taskConfig == null) taskConfig = builder.build();
        return taskConfig;
    }




    protected abstract <INPUT> List<? extends RecurringTaskEntity> findRecurringTasks(Class<? extends Task> taskClass, INPUT input, ScheduledTaskAction scheduledTaskAction, TaskConfig taskConfig) throws Exception;

    protected abstract <INPUT> String doQueueTask(Task<INPUT> task, INPUT input, long startDelayInMilliseconds, Integer priority, RecurringSchedule recurringSchedule) throws Exception;
    protected abstract <INPUT> String doQueueTask(Task<INPUT> task, INPUT input, long startDelayInMilliseconds, Integer priority, RecurringSchedule recurringSchedule, TaskConfig taskConfig) throws Exception;

    protected abstract <INPUT> void updateRecurringTask(RecurringTaskEntity taskEntity, RecurringSchedule recurringSchedule, INPUT input, TaskConfig taskConfig) throws Exception;


    @Override
    public synchronized <INPUT> String registerScheduledTask(Task<INPUT> task, INPUT input, int periodInMilliseconds, ScheduledTaskAction scheduledTaskAction) throws Exception{

        final Class<? extends Task> taskClass = task.getClass();
        final TaskConfig taskConfig = getTaskConfig(task, clusterTasksConfig);
        final var scheduledTasks = findRecurringTasks(taskClass, input, scheduledTaskAction, taskConfig);

        final RecurringSchedule newRecurringSchedule = RecurringSchedule.createNewRecurringSchedule(new RecurringScheduleStrategyFixed(periodInMilliseconds), timeProvider.getCurrent());


        if (scheduledTaskAction == ScheduledTaskAction.AlwaysAdd) {
            return doQueueTask(task, input, 0, null, newRecurringSchedule, taskConfig);
        }

        if (scheduledTaskAction.isReplaceTasks()) {

            if (scheduledTasks.size() > 0) {
                log.info("Replacing all {} recurring tasks {} as part of {} action. Deleting all of them and creating a single new one.", scheduledTasks.size(), taskClass, scheduledTaskAction);
                scheduledTasks.forEach(t -> deleteTask(t.getTaskId()));
            }
            return doQueueTask(task, input, 0, null, newRecurringSchedule);
        }
        else if (!scheduledTaskAction.isReplaceTasks()){ // update tasks

            if (scheduledTasks.size() > 1) {
                log.error("Found {} recurring tasks as part of {} action. Updating all of them.", scheduledTasks.size(), scheduledTaskAction);
            }

            for (RecurringTaskEntity t : scheduledTasks) {
                // TODO: anything else from task?
                // t.priority ??
                // t.taskConfig ??

                log.info("Updating recurring task {} with new input & schedule", t.getTaskId());
                updateRecurringTask(t, newRecurringSchedule, input, taskConfig);
            }


            switch (scheduledTasks.size()) {
                case 0:
                    return doQueueTask(task, input, 0, null, newRecurringSchedule, taskConfig);
                case 1: return scheduledTasks.get(0).getTaskId();
                default:
                    return null; // TODO: return optional, return array???, error?

            }

        } else throw new IllegalArgumentException("Unknown unhandled scheduledTaskAction " + scheduledTaskAction);


    }



}
