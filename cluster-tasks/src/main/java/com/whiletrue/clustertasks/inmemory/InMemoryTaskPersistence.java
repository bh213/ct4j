package com.whiletrue.clustertasks.inmemory;

import com.whiletrue.clustertasks.factory.TaskFactory;
import com.whiletrue.clustertasks.instanceid.ClusterInstanceNaming;
import com.whiletrue.clustertasks.tasks.*;
import com.whiletrue.clustertasks.tasks.recurring.RecurringSchedule;
import com.whiletrue.clustertasks.tasks.recurring.RecurringScheduleStrategyFixed;
import com.whiletrue.clustertasks.timeprovider.TimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.reverseOrder;

public class InMemoryTaskPersistence extends TaskPersistenceBase {
    private static Logger log = LoggerFactory.getLogger(InMemoryTaskPersistence.class);
    private ClusterInstanceNaming clusterInstanceNaming;
    private TaskFactory taskFactory;
    private List<TaskEntry> tasksInQueue;
    private PriorityQueue<TaskEntry> waitingTasks;
    private ClusterTasksConfig clusterTasksConfig;
    private TimeProvider timeProvider;

    public InMemoryTaskPersistence(ClusterInstanceNaming clusterInstanceNaming, TaskFactory taskFactory, ClusterTasksConfig clusterTasksConfig, TimeProvider timeProvider) {
        super(log, timeProvider);
        this.clusterInstanceNaming = clusterInstanceNaming;
        this.taskFactory = taskFactory;
        this.clusterTasksConfig = clusterTasksConfig;
        this.timeProvider = timeProvider;
        tasksInQueue = new ArrayList<>();
        waitingTasks = new PriorityQueue<>(Comparator.comparing(entry -> entry.nextRun));
    }

    private <INPUT> TaskWrapper<INPUT> createTaskWrapper(TaskEntry taskEntry) {
        final Task<INPUT> instance;
        try {
            instance = taskFactory.createInstance(taskEntry.taskClass);
        } catch (Exception e) {
            log.error("Could not find create instance of class {} for task id {}:{}", taskEntry.taskClass, taskEntry.taskId, e);
            taskEntry.taskStatus = TaskStatus.Failure;
            taskEntry.startTime = null;
            throw new RuntimeException(e);
        }
        final TaskExecutionContext taskExecutionContext = new TaskExecutionContext(taskEntry.retryCount, clusterInstanceNaming.getInstanceId(), taskEntry.taskId, taskEntry.name, taskEntry.recurringSchedule);

        return new TaskWrapper<>(instance, (INPUT) taskEntry.input, taskExecutionContext, taskEntry.lastUpdated, taskEntry.taskConfig);
    }

    @Override
    public synchronized List<TaskWrapper<?>> pollForNextTasks(int maxTasks) throws Exception {
        findTasksReadyToRun();
        tasksInQueue.sort(Comparator.comparing(taskEntry -> taskEntry.priority, reverseOrder()));
        return tasksInQueue.stream().filter(e -> e.startTime == null && e.taskStatus == TaskStatus.Pending).map(this::createTaskWrapper).limit(maxTasks).collect(Collectors.toList());
    }

    private void findTasksReadyToRun() {
        while (true) {
            final TaskEntry entry = waitingTasks.peek();
            if (entry == null) break;
            if (entry.nextRun.isBefore(timeProvider.getCurrent())) {
                tasksInQueue.add(waitingTasks.poll());
            } else break;
        }
    }

    @Override
    public synchronized List<TaskWrapper<?>> findClaimedTasks(List<TaskWrapper<?>> tasks) throws Exception {
        return tasks;
    }

    @Override
    public synchronized int tryClaimTasks(List<TaskWrapper<?>> tasks) {
        int claimed = 0;
        final Instant now = timeProvider.getCurrent();
        for (TaskWrapper<?> task : tasks) {
            final TaskEntry entry = findTask(task);
            if (entry.taskStatus == TaskStatus.Pending && entry.startTime == null) {
                entry.startTime = now;
                entry.taskStatus = TaskStatus.Claimed;
                claimed++;
            }
        }
        return claimed;
    }

    @Override
    public <INPUT> String queueTask(Task<INPUT> task, INPUT input) throws Exception {
        return doQueueTask(task, input, 0, null, null);
    }

    @Override
    public <INPUT> String queueTask(Task<INPUT> task, INPUT input, int priority) throws Exception {
        return doQueueTask(task, input, 0, priority, null);
    }

    @Override
    public <INPUT> String queueTaskDelayed(Task<INPUT> task, INPUT input, long startDelayInMilliseconds) throws Exception {
        return doQueueTask(task, input, startDelayInMilliseconds, null, null);
    }

    @Override
    public synchronized <INPUT> String queueTaskDelayed(Task<INPUT> task, INPUT input, long startDelayInMilliseconds, int priority) throws Exception {
        return doQueueTask(task, input, startDelayInMilliseconds, priority, null);
    }

    @Override
    public synchronized <INPUT> String registerScheduledTask(Task<INPUT> task, INPUT input, int periodInMilliseconds, ScheduledTaskAction scheduledTaskAction) {

        final Class<? extends Task> taskClass = task.getClass();
        final List<TaskEntry> scheduledTasks = tasksInQueue.stream().filter(e -> e.taskClass.equals(taskClass) && (!scheduledTaskAction.isPerInput() || Objects.equals(input, e.input))).collect(Collectors.toList());

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


    }

    private synchronized <INPUT> String doQueueTask(Task<INPUT> task, INPUT input, long startDelayInMilliseconds, Integer priority, RecurringSchedule recurringSchedule) {

        TaskConfig taskConfig = getTaskConfig(task, clusterTasksConfig);

        TaskEntry entry = new TaskEntry(taskConfig.getTaskName());
        entry.input = input;

        final Instant current = timeProvider.getCurrent();
        entry.lastUpdated = current;
        entry.priority = priority != null ? priority : taskConfig.getPriority();
        entry.retryCount = 0;
        entry.taskClass = task.getClass();
        entry.taskConfig = taskConfig;
        entry.taskStatus = TaskStatus.Pending;
        entry.recurringSchedule = recurringSchedule;
        if (recurringSchedule != null) {
            entry.nextRun = recurringSchedule.getStrategy().nextRunTime(current).plusMillis(startDelayInMilliseconds);

        } else {
            entry.nextRun = current.plusMillis(startDelayInMilliseconds);
        }

        if (startDelayInMilliseconds == 0) tasksInQueue.add(entry);
        else waitingTasks.add(entry);
        return entry.taskId;
    }

    private synchronized TaskEntry findTask(String taskId) {
        final Optional<TaskEntry> entry = tasksInQueue.stream().filter(e -> e.taskId.equals(taskId)).findFirst();
        return entry.orElseGet(() -> waitingTasks.stream().filter(e -> e.taskId.equals(taskId)).findFirst().orElse(null));
    }

    private TaskEntry findTask(TaskWrapper wrapper) {
        Objects.requireNonNull(wrapper);
        return findTask(wrapper.getTaskExecutionContext().getTaskId());
    }

    @Override
    public synchronized void deleteTask(String id) {
        final TaskEntry task = findTask(id);
        if (task != null) {
            removeTask(task);
        }
    }

    private void removeTask(TaskEntry task) {
        waitingTasks.remove(task);
        tasksInQueue.remove(task);
    }

    @Override
    public synchronized void unlockAndChangeStatus(List<TaskWrapper<?>> tasks, TaskStatus status) {
        for (TaskWrapper<?> task : tasks) {
            final TaskEntry entry = findTask(task);
            if (entry != null) {
                entry.taskStatus = status;
                entry.startTime = null;
                removeTask(entry);
                waitingTasks.add(entry);
            }
        }
    }

    @Override
    public synchronized void unlockAndMarkForRetry(TaskWrapper<?> task, int retryCount, Instant nextRun) {
        final TaskEntry entry = findTask(task);
        entry.retryCount = retryCount;
        entry.nextRun = nextRun;
        entry.taskStatus = TaskStatus.Pending;
        entry.startTime = null;
        removeTask(entry);
        waitingTasks.add(entry);
    }

    @Override
    public void unlockAndMarkForRetryAndSetScheduledNextRun(TaskWrapper<?> task, int retryCount, Instant nextRun, Instant nextScheduledRun) {
        final TaskEntry entry = findTask(task);
        entry.retryCount = retryCount;
        entry.nextRun = nextRun;
        entry.taskStatus = TaskStatus.Pending;
        entry.startTime = null;
        Objects.requireNonNull(entry.recurringSchedule, "recurringSchedule must be set").setNextScheduledRun(nextScheduledRun);
        removeTask(entry);
        waitingTasks.add(entry);
    }


    @Override
    public synchronized TaskWrapper<?> getTask(String taskId) {
        final TaskEntry entry = findTask(Objects.requireNonNull(taskId));
        if (entry == null) return null;
        return createTaskWrapper(entry);
    }

    @Override
    public TaskStatus getTaskStatus(String taskId) {
        final TaskEntry entry = findTask(Objects.requireNonNull(taskId));
        if (entry == null) return null;
        return entry.taskStatus;
    }

    @Override
    public synchronized long countPendingTasks() {
        return tasksInQueue.size();
    }

    @Override
    public ClusterNodePersistence getClusterNodePersistence() {
        return null;
    }

    @Override
    public boolean isClustered() {
        return false;
    }

    @Override
    public boolean isPersistent() {
        return false;
    }

    private class TaskEntry {

        public final String taskId;
        public String name;
        public Class<? extends Task> taskClass;
        public TaskConfig taskConfig;
        public int retryCount;
        public Object input;
        public Instant lastUpdated;
        public int priority;
        public Instant nextRun;
        public RecurringSchedule recurringSchedule;
        public TaskStatus taskStatus;
        public Instant startTime;

        public TaskEntry(String name) {
            this.name = name;
            taskId = UUID.randomUUID().toString();
        }
    }
}
