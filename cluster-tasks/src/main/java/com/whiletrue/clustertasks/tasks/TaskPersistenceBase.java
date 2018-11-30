package com.whiletrue.clustertasks.tasks;

import com.whiletrue.clustertasks.timeprovider.TimeProvider;
import org.slf4j.Logger;

import java.util.Objects;

public abstract  class TaskPersistenceBase implements TaskPersistence {

    protected Logger log;
    protected TimeProvider timeProvider;

    public TaskPersistenceBase(Logger log, TimeProvider timeProvider) {
        this.log = log;
        this.timeProvider = timeProvider;
    }

    public <INPUT> TaskConfig getTaskConfig(Task<INPUT> task, ClusterTasksConfig clusterTasksConfig) {
        final Class<? extends Task> taskClass = Objects.requireNonNull(task).getClass();
        final ClusterTask clusterTaskAnnotation = Utils.getClusterTaskAnnotation(taskClass);
        final TaskConfig.TaskConfigBuilder builder = new TaskConfig.TaskConfigBuilder(clusterTasksConfig, clusterTaskAnnotation, taskClass);
        TaskConfig taskConfig = task.configureTask(builder);
        if (taskConfig == null) taskConfig = builder.build();
        return taskConfig;
    }

    /*protected abstract <INPUT> String doQueueTask(Task<INPUT> task, INPUT input, long startDelayInMilliseconds, Integer priority, RecurringSchedule recurringSchedule) throws Exception;


    @Override
    public synchronized <INPUT> String registerScheduledTask(Task<INPUT> task, INPUT input, int periodInMilliseconds, ScheduledTaskAction scheduledTaskAction) throws Exception{

        final Class<? extends Task> taskClass = task.getClass();
        final List<InMemoryTaskPersistence.TaskEntry> scheduledTasks = tasksInQueue.stream().filter(e -> e.taskClass.equals(taskClass) && (!scheduledTaskAction.isPerInput() || Objects.equals(input, e.input))).collect(Collectors.toList());

        final RecurringSchedule newRecurringSchedule = RecurringSchedule.createNewRecurringSchedule(new RecurringScheduleStrategyFixed(periodInMilliseconds), timeProvider.getCurrent());


        if (scheduledTaskAction == ScheduledTaskAction.AlwaysAdd) {
            return doQueueTask(task, input, 0, null, newRecurringSchedule);
        }

        if (scheduledTaskAction.isReplaceTasks()) {

            if (scheduledTasks.size() > 0) {
                log.info("Replacing all {} recurring tasks {} as part of {} action. Deleting all of them and creating a single new one.", scheduledTasks.size(), taskClass, scheduledTaskAction);
                scheduledTasks.forEach(t -> deleteTask(t.taskId));
            }
            return doQueueTask(task, input, 0, null, newRecurringSchedule);
        }
        else if (!scheduledTaskAction.isReplaceTasks()){ // update tasks

            if (scheduledTasks.size() > 1) {
                log.error("Found {} recurring tasks as part of {} action. Updating all of them.", scheduledTasks.size(), scheduledTaskAction);
            }

            scheduledTasks.forEach(t -> {
                // TODO: anything else from task?
                // t.priority ??
                // t.taskConfig ??

                log.info("Updating recurring task {} with new input & schedule", t.taskId);


                final String newStrategyString = newRecurringSchedule.getStrategy().toDatabaseString();
                if (!(Objects.equals(t.input, input) &&t.recurringSchedule.getStrategy().toDatabaseString().equals(newStrategyString))) {
                    log.info("Task {} input and/or schedule was changed, updating", t.taskId);
                    t.input = input;
                    t.recurringSchedule = newRecurringSchedule;
                } else  {
                    log.info("Task {} input and/or schedule was not changed, no operation performed", t.taskId);
                }

            });

            switch (scheduledTasks.size()) {
                case 0:
                    return doQueueTask(task, input, 0, null, newRecurringSchedule);
                case 1: return scheduledTasks.get(0).taskId;
                default:
                    return null; // TODO: return optional, return array???, error?

            }

        } else throw new IllegalArgumentException("Unknown unhandled scheduledTaskAction " + scheduledTaskAction);


    }*/



}
