package com.whiletrue.clustertasks.inmemory;

import com.whiletrue.clustertasks.factory.TaskFactory;
import com.whiletrue.clustertasks.instanceid.ClusterInstance;
import com.whiletrue.clustertasks.tasks.*;
import com.whiletrue.clustertasks.timeprovider.TimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.reverseOrder;

public class InMemoryTaskPersistence implements TaskPersistence {
    private static Logger log = LoggerFactory.getLogger(InMemoryTaskPersistence.class);
    private ClusterInstance clusterInstance;
    private TaskFactory taskFactory;
    private List<TaskEntry> tasksInQueue;
    private PriorityQueue<TaskEntry> waitingTasks;
    private ClusterTasksConfig clusterTasksConfig;
    private TimeProvider timeProvider;

    public InMemoryTaskPersistence(ClusterInstance clusterInstance, TaskFactory taskFactory, ClusterTasksConfig clusterTasksConfig, TimeProvider timeProvider) {
        this.clusterInstance = clusterInstance;
        this.taskFactory = taskFactory;
        this.clusterTasksConfig = clusterTasksConfig;
        this.timeProvider = timeProvider;
        tasksInQueue = new ArrayList<>();
        waitingTasks = new PriorityQueue<>(Comparator.comparing(entry -> entry.nextRun));
    }

    private <INPUT> TaskWrapper<INPUT> createTaskWrapper(TaskEntry taskEntry) {
        final Task instance;
        try {
            instance = taskFactory.createInstance(taskEntry.taskClass);
        } catch (Exception e) {
            log.error("Could not find create instance of class {} for task id {}:{}", taskEntry.taskClass, taskEntry.taskId, e);
            taskEntry.taskStatus = TaskStatus.Failure;
            taskEntry.startTime = null;
            throw new RuntimeException(e);
        }
        final TaskExecutionContext taskExecutionContext = new TaskExecutionContext(taskEntry.retryCount, clusterInstance.getInstanceId(), taskEntry.taskId, taskEntry.name);

        return new TaskWrapper(instance, taskEntry.input, taskExecutionContext, taskEntry.lastUpdated, taskEntry.taskConfig);
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
        return doQueueTask(task, input, 0, null);
    }

    @Override
    public <INPUT> String queueTask(Task<INPUT> task, INPUT input, int priority) throws Exception {
        return doQueueTask(task, input, 0, priority);
    }

    @Override
    public <INPUT> String queueTaskDelayed(Task<INPUT> task, INPUT input, long startDelayInMilliseconds) throws Exception {
        return doQueueTask(task, input, startDelayInMilliseconds, null);
    }

    @Override
    public synchronized <INPUT> String queueTaskDelayed(Task<INPUT> task, INPUT input, long startDelayInMilliseconds, int priority) throws Exception {
        return doQueueTask(task, input, startDelayInMilliseconds, priority);
    }

    private <INPUT> String doQueueTask(Task<INPUT> task, INPUT input, long startDelayInMilliseconds, Integer priority) {
        final Class<? extends Task> taskClass = Objects.requireNonNull(task).getClass();
        final ClusterTask clusterTaskAnnotation = Utils.getClusterTaskAnnotation(taskClass);
        final TaskConfig.TaskConfigBuilder builder = new TaskConfig.TaskConfigBuilder(clusterTasksConfig, clusterTaskAnnotation, taskClass);
        TaskConfig taskConfig = task.configureTask(builder);
        if (taskConfig == null) taskConfig = builder.build();

        TaskEntry entry = new TaskEntry(taskConfig.getTaskName());
        entry.input = input;

        entry.lastUpdated = timeProvider.getCurrent();
        entry.nextRun = timeProvider.getCurrent().plusMillis(startDelayInMilliseconds);
        entry.priority = priority != null ? priority : taskConfig.getPriority();
        entry.retryCount = 0;
        entry.taskClass = taskClass;
        entry.taskConfig = taskConfig;
        entry.taskStatus = TaskStatus.Pending;

        if (startDelayInMilliseconds == 0) tasksInQueue.add(entry);
        else waitingTasks.add(entry);
        return entry.taskId;
    }

    private TaskEntry findTask(String taskId) {
        final Optional<TaskEntry> entry = tasksInQueue.stream().filter(e -> e.taskId == taskId).findFirst();
        return entry.orElseGet(() -> waitingTasks.stream().filter(e -> e.taskId == taskId).findFirst().orElse(null));
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
    public synchronized void unlockAndMarkForRetry(TaskWrapper<?> task, int retryCount, Instant newScheduledTime) {
        final TaskEntry entry = findTask(task);
        entry.retryCount = retryCount;
        entry.nextRun = newScheduledTime;
        entry.taskStatus = TaskStatus.Pending;
        entry.startTime = null;
        removeTask(entry);
        waitingTasks.add(entry);
    }

    @Override
    public TaskWrapper<?> getTask(String taskId) {
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
        public TaskStatus taskStatus;
        public Instant startTime;
        public TaskEntry(String name) {
            this.name = name;
            taskId = UUID.randomUUID().toString();
        }
    }
}
