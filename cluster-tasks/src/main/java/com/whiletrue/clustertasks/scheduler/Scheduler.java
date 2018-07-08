package com.whiletrue.clustertasks.scheduler;

import com.whiletrue.clustertasks.tasks.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Scheduler implements InternalTaskEvents {


    public static final String CLUSTER_TASKS_SCHEDULER_THREAD = "Cluster tasks scheduler thread";
    private static Logger log = LoggerFactory.getLogger(Scheduler.class);
    private final Lock lock = new ReentrantLock();
    private final Condition waitingForPolling = lock.newCondition();
    private final AtomicBoolean isSchedulerThreadRunning = new AtomicBoolean(true);
    private TaskPersistence taskPersistence;
    private TaskRunner taskRunner;
    private Thread schedulerThread;
    private ClusterTasksConfig clusterTasksConfig;
    private TaskPerformanceEventsCollector taskPerformanceEventsCollector;
    private Instant lastPollingTime;
    private Instant nextPollingTime;



    public Scheduler(TaskPersistence taskPersistence, TaskRunner taskRunner, ClusterTasksConfig clusterTasksConfig) {
        this.taskPersistence = taskPersistence;
        this.taskRunner = taskRunner;
        this.clusterTasksConfig = clusterTasksConfig;
        this.taskPerformanceEventsCollector = new TaskPerformanceEventsCollector();
    }


    private void schedulerThreadMain() {
        while (isSchedulerThreadRunning.get()) {

            try {
                if (hasMinimumPollingTimeElapsed()) {
                    try {
                        pollForTasks(calculateNumberOfTasksToAskFor());
                        lastPollingTime = Instant.now();
                        nextPollingTime = null;
                    } catch (Exception e) {
                        log.error("Error in scheduler task, pollForTasks", e);
                    }
                }

                final int nextPollingTimeInMilliseconds = getNextPollingTimeInMilliseconds();
                log.info("next polling {}", nextPollingTimeInMilliseconds);
                lock.lock();
                try {
                    waitingForPolling.await(nextPollingTimeInMilliseconds, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    log.info("scheduler thread interrupted", e); // TODO: Remove this
                } finally {
                    lock.unlock();
                }


            } catch (Exception e) {
                log.error("Error in schedulerThreadMain", e);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e1) {

                }
            }
        }
    }


    private synchronized boolean hasMinimumPollingTimeElapsed() {
        if (lastPollingTime == null) return true;
        final long sinceLastPollingTime = Duration.between(lastPollingTime, Instant.now()).toMillis();
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
            final Instant now = Instant.now();
            if (nextPollingTime.isBefore(now)) return 1;

            final int tillNextForcedPoll = (int) Duration.between(now, nextPollingTime).toMillis();
            return tillNextForcedPoll + 1;
        }
        return clusterTasksConfig.getMaximumPollingTimeMilliseconds();
    }


    private synchronized void pollForTasks(int maxTasks) {
        try {
            log.debug("Polling for tasks");
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
                if (currentResourcesAvailable.addIfResourcesAreAvailable(candidate.getTaskConfig().getResourceUsage())){
                    log.debug("Will try to claim task {}", candidate.getTaskExecutionContext().getTaskId());
                    candidatesAfterResources.add(candidate);
                } else {
                    log.debug("Not enough resources to claim task {}", candidate.getTaskExecutionContext().getTaskId());
                }

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

                if (!claimedTask.getLastUpdated().equals(candidateTask.get())) {
                    claimedTask = taskPersistence.getTask(claimedTask.getTaskExecutionContext().getTaskId());
                }
                taskRunner.executeTask(claimedTask, this);
            }
        } catch (Exception e) {
            log.error("Error while polling for tasks", e);
            return;
        }
    }

    public synchronized void stopScheduling() {
        log.info("stopping scheduling thread");

        isSchedulerThreadRunning.set(false);
        lock.lock();
        try {
            waitingForPolling.signal();
        } finally {
            lock.unlock();
        }

        try {
            schedulerThread.join(100L);
        } catch (InterruptedException e) {
            schedulerThread.interrupt();
            schedulerThread = null;
        }
        schedulerThread = null;
    }

    public synchronized void startScheduling() {
        log.info("Starting scheduling thread");
        isSchedulerThreadRunning.set(true);
        schedulerThread = new Thread(null, this::schedulerThreadMain, CLUSTER_TASKS_SCHEDULER_THREAD);
        schedulerThread.setPriority(Thread.NORM_PRIORITY);
        schedulerThread.setUncaughtExceptionHandler((t, e) -> {
            log.error("scheduler thread caught uncaught exception", e);
        });
        schedulerThread.start();
    }

    @Override
    public void taskStarted(Class<? extends Task> name, String id) {
        taskPerformanceEventsCollector.taskStarted(name, id);
    }

    @Override
    public void taskCompleted(Class<? extends Task> name, String id, int retry, float milliseconds) {

        taskPerformanceEventsCollector.taskCompleted(name, id, retry, milliseconds);

        lock.lock();
        try {
            waitingForPolling.signal();
        } finally {
            lock.unlock();
        }

        if (nextPollingTime == null) synchronized (this) {
            nextPollingTime = Instant.now().plusMillis(clusterTasksConfig.getMinimumPollingTimeMilliseconds());
        }

    }

    @Override
    public void taskError(Class<? extends Task> name, String id, int retry, float milliseconds) {
        taskPerformanceEventsCollector.taskError(name, id, retry, milliseconds);
        lock.lock();
        try {
            waitingForPolling.signal();
        } finally {
            lock.unlock();
        }
        if (nextPollingTime == null) synchronized (this) {
            nextPollingTime = Instant.now().plusMillis(clusterTasksConfig.getMinimumPollingTimeMilliseconds());
        }
    }

    @Override
    public void taskFailed(Class<? extends Task> name, String id, int retry) {
        taskPerformanceEventsCollector.taskFailed(name, id, retry);

        lock.lock();
        try {
            waitingForPolling.signal();
        } finally {
            lock.unlock();
        }

        if (nextPollingTime == null) synchronized (this) {
            nextPollingTime = Instant.now().plusMillis(clusterTasksConfig.getMinimumPollingTimeMilliseconds());
        }


    }

    public ResourceUsage getFreeResourcesEstimate() {
        return taskRunner.getCurrentResourcesAvailable();
    }
}
