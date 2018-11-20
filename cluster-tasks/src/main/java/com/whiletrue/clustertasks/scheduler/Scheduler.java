package com.whiletrue.clustertasks.scheduler;

import com.whiletrue.clustertasks.instanceid.ClusterInstance;
import com.whiletrue.clustertasks.tasks.*;
import com.whiletrue.clustertasks.timeprovider.TimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class Scheduler implements InternalTaskEvents {

    private static final String CLUSTER_TASKS_SCHEDULER_THREAD = "ct4j-scheduler";
    private static final String CLUSTER_TASKS_HEARTBEAT_THREAD = "ct4j-heartbeat";
    private static Logger log = LoggerFactory.getLogger(Scheduler.class);
    private final Lock schedulerLock = new ReentrantLock();
    private final Lock checkInLock = new ReentrantLock();
    private final Condition waitingForPolling = schedulerLock.newCondition();
    private final Condition waitingForClusterCheckIn = checkInLock.newCondition();
    private final AtomicBoolean isSchedulerThreadRunning = new AtomicBoolean(false);
    private final TaskPerformanceEventsCollector taskPerformanceEventsCollector;
    private final Executor callbackExecutor;
    private TaskPersistence taskPersistence;
    private TaskRunner taskRunner;
    private Thread schedulerThread;
    private Thread checkinThread;
    private ClusterTasksConfig clusterTasksConfig;
    private SchedulerCallbackListener callbackListener;
    private Instant lastPollingTime;
    private Instant nextPollingTime;
    private TimeProvider timeProvider;

    public Scheduler(TaskPersistence taskPersistence, TaskRunner taskRunner, ClusterTasksConfig clusterTasksConfig, TimeProvider timeProvider) {
        this.taskPersistence = taskPersistence;
        this.taskRunner = taskRunner;
        this.clusterTasksConfig = clusterTasksConfig;
        this.timeProvider = timeProvider;
        this.taskPerformanceEventsCollector = new TaskPerformanceEventsCollector();
        this.callbackExecutor = Executors.newCachedThreadPool();
    }

    public Instant getNextPollingTime() {
        return nextPollingTime;
    }

    private void schedulerThreadMain() {
        while (isSchedulerThreadRunning.get()) {

            try {
                if (hasMinimumPollingTimeElapsed()) {
                    try {
                        pollForTasks(calculateNumberOfTasksToAskFor());
                        lastPollingTime = timeProvider.getCurrent();
                        nextPollingTime = null;

                    } catch (Exception e) {
                        log.error("Error in scheduler task, pollForTasks", e);
                    }
                }

                final int nextPollingTimeInMilliseconds = getNextPollingTimeInMilliseconds();
                log.debug("next polling {}", nextPollingTimeInMilliseconds);

                overdueTaskCheck();

                schedulerLock.lock();
                try {
                    waitingForPolling.await(nextPollingTimeInMilliseconds, TimeUnit.MILLISECONDS);
                } catch (InterruptedException ignored) {
                    // ignore
                } finally {
                    schedulerLock.unlock();
                }

            } catch (Exception e) {
                log.error("Error in schedulerThreadMain", e);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {

                }
            }
        }
        log.info("Exiting ct4j scheduler thread");
    }



    private void checkForNodeChanges(List<ClusterInstance> previousInstances, List<ClusterInstance> currentInstances) {

        if (callbackListener == null) return;

        if (currentInstances == null) currentInstances = Collections.emptyList();
        if (previousInstances == null) previousInstances = Collections.emptyList();

        Map<String, ClusterInstance> currentInstancesMap = currentInstances.stream().collect(Collectors.toMap(ClusterInstance::getInstanceId, x -> x));
        Map<String, ClusterInstance> previousInstancesMap = previousInstances.stream().collect(Collectors.toMap(ClusterInstance::getInstanceId, x -> x));


        final HashMap<String, ClusterInstance>  nodesAdded  = new HashMap<>(currentInstancesMap);
        for (ClusterInstance previousInstance : previousInstances) {
            nodesAdded.remove(previousInstance.getInstanceId());
        }

        for (ClusterInstance clusterInstance : nodesAdded.values()) {
            callbackExecutor.execute(() -> callbackListener.clusterNodeStarted(new ClusterInstance(clusterInstance)));
        }

        final HashMap<String, ClusterInstance>  nodesRemoved = new HashMap<>(previousInstancesMap);
        for (ClusterInstance currentInstance: currentInstances) {
            nodesRemoved.remove(currentInstance.getInstanceId());
        }

        for (ClusterInstance clusterInstance : nodesRemoved.values()) {
            callbackExecutor.execute(() -> callbackListener.clusterNodeStopped(new ClusterInstance(clusterInstance)));
        }

    }



    private void schedulerClusterHeartbeatThread() {

        if (!taskPersistence.isClustered()) {
            log.error("cluster checkin should only be enabled when persistence is clustered");
            return;
        }

        // TODO: specify db connection properties (transactions off and such?)

        String baseUniqueRequestId = UUID.randomUUID().toString() + "-";
        int uniqueRequestCount = 1;

        final ClusterNodePersistence clustered = taskPersistence.getClusterNodePersistence();

        String currentUniqueRequestId = baseUniqueRequestId+uniqueRequestCount;
        clustered.instanceInitialCheckIn(currentUniqueRequestId); // TODO: repeat on failure?
        List<ClusterInstance> instances = null;

        while (isSchedulerThreadRunning.get()) {

            try {
                try {
                    checkInLock.lock();


                    String oldUniqueRequestId = currentUniqueRequestId;
                    uniqueRequestCount++;
                    currentUniqueRequestId = baseUniqueRequestId+uniqueRequestCount;

                    List<ClusterInstance> previousInstances = instances;
                    instances = clustered.instanceHeartbeat(previousInstances, oldUniqueRequestId, currentUniqueRequestId);

                    checkForNodeChanges(previousInstances, instances);

                    log.info("Cluster instance heartbeat: {}",  currentUniqueRequestId);
                    try {
                        waitingForClusterCheckIn.await(clusterTasksConfig.getInstanceCheckinTimeInMilliseconds(), TimeUnit.MILLISECONDS);
                    } finally {
                        checkInLock.unlock();
                    }
                } catch (InterruptedException e) {
                    log.debug("check-in thread interrupted", e);
                }

            } catch (Exception e) {
                log.error("Error in schedulerClusterHeartbeatThread", e);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {

                }
            }
        }

        clustered.instanceFinalCheckOut(currentUniqueRequestId);
        log.info("Exiting check-in thread");
    }

    public synchronized SchedulerCallbackListener getCallbackListener() {
        return this.callbackListener;
    }

    public synchronized void setCallbackListener(SchedulerCallbackListener callbackListener) {
        this.callbackListener = callbackListener;
    }



    private void overdueTaskCheck() {
        if (callbackListener == null) return;
        final List<BasicTaskInfo> overdueTasks = taskRunner.getOverdueTasks();
        final SchedulerCallbackListener callbackListener = getCallbackListener();

        for (BasicTaskInfo task : overdueTasks) {
            callbackExecutor.execute(() -> callbackListener.taskOverdue(task));
        }
    }

    private synchronized boolean hasMinimumPollingTimeElapsed() {
        if (lastPollingTime == null) return true;
        final long sinceLastPollingTime = Duration.between(lastPollingTime, timeProvider.getCurrent()).toMillis();
        return sinceLastPollingTime > clusterTasksConfig.getMinimumPollingTimeMilliseconds();
    }

    public TaskPerformanceStatsSnapshot getPerformanceSnapshot() {
        return taskPerformanceEventsCollector.getPerformanceSnapshot();
    }

    private int calculateNumberOfTasksToAskFor() {
        return taskRunner.getFreeTasksSlots();
    }

    private synchronized int getNextPollingTimeInMilliseconds() {
        if (nextPollingTime != null) {
            final Instant now = timeProvider.getCurrent();
            if (nextPollingTime.isBefore(now)) return 1;

            final int tillNextForcedPoll = (int) Duration.between(now, nextPollingTime).toMillis();
            return tillNextForcedPoll + 1;
        }
        return clusterTasksConfig.getMaximumPollingTimeMilliseconds();
    }

    synchronized void pollForTasks(int maxTasks) {
        try {
            log.debug("Polling for tasks, maximum {}", maxTasks);
            List<TaskWrapper<?>> candidates = taskPersistence.pollForNextTasks(maxTasks);
            final int tasksCount = candidates.size();
            if (tasksCount == 0) {
                log.trace("Got {} tasks", tasksCount);
                return;
            } else {
                log.debug("Got {} tasks to evaluate", tasksCount);
            }

            final ResourceUsage currentResourcesAvailable = taskRunner.getCurrentResourcesAvailable();

            List<TaskWrapper<?>> candidatesAfterResources = new ArrayList<>();

            for (TaskWrapper<?> candidate : candidates) {
                final ResourceUsage taskResourceUsage = candidate.getTaskConfig().getResourceUsage();
                if (currentResourcesAvailable.addIfResourcesAreAvailable(taskResourceUsage)) {
                    log.info("Will try to claim task {}", candidate.getTaskExecutionContext().getTaskId());
                    candidatesAfterResources.add(candidate);
                } else {
                    log.info("Not enough resources to claim task {}", candidate.getTaskExecutionContext().getTaskId());
                    if (!clusterTasksConfig.isSchedulerFitAsManyTaskAsPossible()) {

                        if (clusterTasksConfig.getConfiguredResources().canFit(taskResourceUsage)) {
                            log.warn("Task {} requires more resources than this node provides. Max: {}, Provided: {}", candidate.getTaskExecutionContext().getTaskId(), clusterTasksConfig.getConfiguredResources(), taskResourceUsage);
                            if (callbackListener != null) {
                                callbackExecutor.execute(() -> callbackListener.taskCannotBeScheduled(new BasicTaskInfo(candidate)));
                            }
                        }
                        break;
                    }
                }
            }

            if (candidatesAfterResources.size() == 0) {
                log.debug("No tasks to claim");
                return;
            }

            final int claimedCount = taskPersistence.tryClaimTasks(candidatesAfterResources);
            List<TaskWrapper<?>> claimedTasks = taskPersistence.findClaimedTasks(candidatesAfterResources);

            /*if (claimedCount != tasksCount) {
            }
*/
            log.info("Claimed {} tasks", claimedTasks.size());
            for (TaskWrapper<?> claimedTask : claimedTasks) {

                TaskWrapper<?> finalClaimedTask = claimedTask;
                final Optional<TaskWrapper<?>> candidateTask = candidatesAfterResources.stream().filter(x -> x.getTaskExecutionContext().getTaskId().equals(finalClaimedTask.getTaskExecutionContext().getTaskId())).findFirst();

                if (!claimedTask.getLastUpdated().equals(candidateTask.get().getLastUpdated())) { // TODO: check this
                    claimedTask = taskPersistence.getTask(claimedTask.getTaskExecutionContext().getTaskId());
                }
                taskRunner.executeTask(claimedTask, this);
            }
        } catch (Exception e) {
            log.error("Error while polling for tasks", e);
            return;
        }
    }

    public boolean isSchedulerRunning() {
        return isSchedulerThreadRunning.get();
    }


    public synchronized void stopScheduling() {
        log.info("Stopping ct4j scheduling thread");

        // TODO: calculate max wait time

        boolean currentState = isSchedulerThreadRunning.getAndSet(false);
        if (!currentState) {
            log.error("ct4j scheduler already stopped");
            return;
        }

        schedulerLock.lock();
        try {
            waitingForPolling.signal();
        } finally {
            schedulerLock.unlock();
        }

        checkInLock.lock();
        try {
            waitingForClusterCheckIn.signal();
        } finally {
            checkInLock.unlock();
        }

        if (schedulerThread != null) {
            try {
                schedulerThread.join(500L); // TODO: Add to config, wait for tasks to finish first?
                schedulerThread.interrupt();
            } catch (InterruptedException e) {
                schedulerThread.interrupt();
                schedulerThread = null;
            }
        }
        schedulerThread = null;

        if (checkinThread != null) {
            try {
                checkinThread.join(100L);
                checkinThread.interrupt();
            } catch (InterruptedException e) {
                checkinThread.interrupt();
                checkinThread = null;
            }
        }
        checkinThread = null;
    }

    public synchronized void startScheduling() {

        boolean currentState = isSchedulerThreadRunning.getAndSet(true);
        if (currentState) {
            log.warn("ct4j scheduler already running");
            return;
        }

        log.info("Starting ct4j scheduling thread");
        schedulerThread = new Thread(null, this::schedulerThreadMain, CLUSTER_TASKS_SCHEDULER_THREAD);
        schedulerThread.setPriority(Thread.NORM_PRIORITY); // TODO: higher priority? configurable?
        schedulerThread.setUncaughtExceptionHandler((t, e) -> {
            log.error("ct4j scheduler thread caught uncaught exception", e);
            // TODO: restart?
        });
        schedulerThread.start();

        if (taskPersistence.isClustered()) {
            log.info("Starting ct4j scheduling cluster check-in thread");
            checkinThread = new Thread(null, this::schedulerClusterHeartbeatThread, CLUSTER_TASKS_HEARTBEAT_THREAD);
            checkinThread.setPriority(Thread.MIN_PRIORITY); // TODO: higher priority? configurable?
            checkinThread.setUncaughtExceptionHandler((t, e) -> {
                log.error("ct4j check-in thread caught uncaught exception", e);
                // TODO: restart?
            });
            checkinThread.start();
        }
    }


    private void updatePollingTime() {
        if (!clusterTasksConfig.isSchedulerPollAfterTaskCompletion()) return;
        schedulerLock.lock();
        try {
            waitingForPolling.signal();
        } finally {
            schedulerLock.unlock();
        }
        if (nextPollingTime == null) synchronized (this) {
            nextPollingTime = timeProvider.getCurrent().plusMillis(clusterTasksConfig.getMinimumPollingTimeMilliseconds());
        }
    }


    public List<ClusterInstance> getClusterInstances() {
        final ClusterNodePersistence clustered = taskPersistence.getClusterNodePersistence();
        if (clustered == null) return null;
        return clustered.getClusterInstances();
    }

    @Override
    public void taskStarted(TaskWrapper<?> taskWrapper) {
        taskPerformanceEventsCollector.taskStarted(taskWrapper);
    }

    @Override
    public void taskCompleted(TaskWrapper<?> taskWrapper, int retry, float durationMilliseconds) {
        taskPerformanceEventsCollector.taskCompleted(taskWrapper, retry, durationMilliseconds);
        updatePollingTime();

        final SchedulerCallbackListener callbackListener = this.callbackListener;
        if (callbackListener != null) {
            callbackExecutor.execute(() -> callbackListener.taskCompleted(new BasicTaskInfo(taskWrapper)));
        }

    }

    @Override
    public void taskError(TaskWrapper<?> taskWrapper, int retry, float durationMilliseconds) {
        taskPerformanceEventsCollector.taskError(taskWrapper, retry, durationMilliseconds);
        updatePollingTime();
    }

    @Override
    public void taskFailed(TaskWrapper<?> taskWrapper, int retry) {
        taskPerformanceEventsCollector.taskFailed(taskWrapper, retry);
        updatePollingTime();
        final SchedulerCallbackListener callbackListener = this.callbackListener;
        if (callbackListener != null)
            callbackExecutor.execute(() -> callbackListener.taskFailed(new BasicTaskInfo(taskWrapper)));
    }

    public ResourceUsage getFreeResourcesEstimate() {
        return taskRunner.getCurrentResourcesAvailable();
    }
}
