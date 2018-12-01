package com.whiletrue.clustertasks.inmemory;

import com.whiletrue.clustertasks.factory.TaskFactory;
import com.whiletrue.clustertasks.instanceid.ClusterInstanceNaming;
import com.whiletrue.clustertasks.tasks.*;
import com.whiletrue.clustertasks.tasks.recurring.RecurringSchedule;
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
    private TimeProvider timeProvider;

    public InMemoryTaskPersistence(ClusterInstanceNaming clusterInstanceNaming, TaskFactory taskFactory, ClusterTasksConfig clusterTasksConfig, TimeProvider timeProvider) {
        super(log, timeProvider, clusterTasksConfig);
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
            if (entry == null) continue;
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
    protected <INPUT> void updateRecurringTask(RecurringTaskEntity taskEntity, RecurringSchedule recurringSchedule, INPUT input, TaskConfig taskConfig) {
        TaskEntry t = (TaskEntry) taskEntity;
        final String newStrategyString = recurringSchedule.getStrategy().toDatabaseString();
        if (!(Objects.equals(t.input, input) &&t.recurringSchedule.getStrategy().toDatabaseString().equals(newStrategyString))) {
            log.info("Task {} input and/or schedule was changed, updating", t.taskId);
            t.input = input;
            t.recurringSchedule = recurringSchedule;
        } else  {
            log.info("Task {} input and/or schedule was not changed, no operation performed", t.taskId);
        }
    }

    @Override
    protected <INPUT> List<? extends RecurringTaskEntity> findRecurringTasks(Class<? extends Task> taskClass, INPUT input, ScheduledTaskAction scheduledTaskAction, TaskConfig taskConfig) {
        return tasksInQueue.stream().filter(t -> t.recurringSchedule != null && t.taskClass.equals(taskClass) && (!scheduledTaskAction.isPerInput() || Objects.equals(input, t.input))).collect(Collectors.toList());
    }


    protected synchronized <INPUT> String doQueueTask(Task<INPUT> task, INPUT input, long startDelayInMilliseconds, Integer priority, RecurringSchedule recurringSchedule) {

        TaskConfig taskConfig = getTaskConfig(task, clusterTasksConfig);
        return doQueueTask(task, input, startDelayInMilliseconds, priority, recurringSchedule, taskConfig);
    }

    protected <INPUT> String doQueueTask(Task<INPUT> task, INPUT input, long startDelayInMilliseconds, Integer priority, RecurringSchedule recurringSchedule, TaskConfig taskConfig) {
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
    public synchronized boolean unlockAndChangeStatus(List<TaskWrapper<?>> tasks, TaskStatus status) {
        boolean retVal = false;
        for (TaskWrapper<?> task : tasks) {
            final TaskEntry entry = findTask(task);
            if (entry != null) {
                entry.taskStatus = status;
                entry.startTime = null;
                removeTask(entry);
                waitingTasks.add(entry);
                retVal = true;
            };
        }
        return retVal;
    }

    @Override
    public synchronized boolean unlockAndMarkForRetry(TaskWrapper<?> task, int retryCount, Instant nextRun) {
        return unlockAndMarkForRetryImpl(task, retryCount, nextRun, null, false);
    }

    @Override
    public synchronized boolean unlockAndMarkForRetryAndSetScheduledNextRun(TaskWrapper<?> task, int retryCount, Instant nextRun, Instant nextScheduledRun) {
        return unlockAndMarkForRetryImpl(task, retryCount, nextRun, nextScheduledRun, true);
    }

    private boolean unlockAndMarkForRetryImpl(TaskWrapper<?> task, int retryCount, Instant nextRun, Instant nextScheduledRun, boolean isRecurring) {
        final TaskEntry entry = findTask(task);
        if (entry == null) return false;
        entry.retryCount = retryCount;
        entry.nextRun = nextRun;
        entry.taskStatus = TaskStatus.Pending;
        entry.startTime = null;
        if (isRecurring) Objects.requireNonNull(entry.recurringSchedule, "recurringSchedule must be set").setNextScheduledRun(nextScheduledRun);
        removeTask(entry);
        waitingTasks.add(entry);
        return true;
    }


    @Override
    public synchronized TaskWrapper<?> getTask(String taskId) {
        final TaskEntry entry = findTask(Objects.requireNonNull(taskId));
        if (entry == null) return null;
        return createTaskWrapper(entry);
    }

    @Override
    public synchronized TaskStatus getTaskStatus(String taskId) {
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

    private class TaskEntry implements RecurringTaskEntity{

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

        @Override
        public String getTaskId() {
            return taskId;
        }
    }
}
