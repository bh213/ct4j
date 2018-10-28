package com.whiletrue.clustertasks.scheduler;

import com.whiletrue.clustertasks.tasks.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Scheduler implements InternalTaskEvents {

    private static final String CLUSTER_TASKS_SCHEDULER_THREAD = "ct4j-scheduler";
    private static Logger log = LoggerFactory.getLogger(Scheduler.class);
    private final Lock lock = new ReentrantLock();
    private final Condition waitingForPolling = lock.newCondition();
    private final AtomicBoolean isSchedulerThreadRunning = new AtomicBoolean(false);
    private TaskPersistence taskPersistence;
    private TaskRunner taskRunner;
    private Thread schedulerThread;
    private ClusterTasksConfig clusterTasksConfig;
    private TaskPerformanceEventsCollector taskPerformanceEventsCollector;
    private SchedulerCallbackListener callbackListener;
    private final Executor callbackExecutor;

    private Instant lastPollingTime;
    private Instant nextPollingTime;

    public Scheduler(TaskPersistence taskPersistence, TaskRunner taskRunner, ClusterTasksConfig clusterTasksConfig) {
        this.taskPersistence = taskPersistence;
        this.taskRunner = taskRunner;
        this.clusterTasksConfig = clusterTasksConfig;
        this.taskPerformanceEventsCollector = new TaskPerformanceEventsCollector();
        this.callbackExecutor = Executors.newSingleThreadExecutor();
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

                overdueTaskCheck();

                lock.lock();
                try {
                    waitingForPolling.await(nextPollingTimeInMilliseconds, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    log.debug("scheduler thread interrupted", e); // TODO: Remove this
                } finally {
                    lock.unlock();
                }

            } catch (Exception e) {
                log.error("Error in schedulerThreadMain", e);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {

                }
            }
        }
        log.info("Exiting scheduler thread");
    }

    public synchronized void setCallbackListener(SchedulerCallbackListener callbackListener) {
        this.callbackListener = callbackListener;
    }

    public synchronized SchedulerCallbackListener getCallbackListener() {
        return this.callbackListener;
    }


    private void overdueTaskCheck() {
        if (callbackListener == null) return;
        final List<BasicTaskInfo> overdueTasks = taskRunner.getOverdueTasks();
        final SchedulerCallbackListener callbackListener = getCallbackListener();

        for(BasicTaskInfo task : overdueTasks) {
            callbackExecutor.execute(()->callbackListener.taskOverdue(task));
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

    public boolean isSchedulerRunning() {
        return isSchedulerThreadRunning.get();
    }

    public synchronized void stopScheduling() {
        log.info("stopping scheduling thread");

        boolean currentState = isSchedulerThreadRunning.getAndSet(false);
        if (!currentState) {
            log.warn("Scheduler already stopped");
            return;
        }

        lock.lock();
        try {
            waitingForPolling.signal();
        } finally {
            lock.unlock();
        }

        if (schedulerThread != null) {
            try {
                schedulerThread.join(500L);
            } catch (InterruptedException e) {
                schedulerThread.interrupt();
                schedulerThread = null;
            }
        }
        schedulerThread = null;
    }

    public synchronized void startScheduling() {
        log.info("Starting scheduling thread");
        boolean currentState = isSchedulerThreadRunning.getAndSet(true);
        if (currentState) {
            log.warn("Scheduler already running");
            return;
        }

        schedulerThread = new Thread(null, this::schedulerThreadMain, CLUSTER_TASKS_SCHEDULER_THREAD);
        schedulerThread.setPriority(Thread.NORM_PRIORITY); // TODO: higher priority? configurable?
        schedulerThread.setUncaughtExceptionHandler((t, e) -> {
            log.error("scheduler thread caught uncaught exception", e);
            // TODO: restart?
        });
        schedulerThread.start();
    }


    private void updatePollingTime() {
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
    public void taskStarted(TaskWrapper<?> taskWrapper) {
        taskPerformanceEventsCollector.taskStarted(taskWrapper);

    }

    @Override
    public void taskCompleted(TaskWrapper<?> taskWrapper, int retry, float durationMilliseconds) {
        taskPerformanceEventsCollector.taskCompleted(taskWrapper, retry, durationMilliseconds);
        updatePollingTime();
        if (callbackExecutor != null) callbackExecutor.execute(()->callbackListener.taskCompleted(new BasicTaskInfo(taskWrapper)));
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
        if (callbackExecutor != null) callbackExecutor.execute(()->callbackListener.taskFailed(new BasicTaskInfo(taskWrapper)));
    }

    public ResourceUsage getFreeResourcesEstimate() {
        return taskRunner.getCurrentResourcesAvailable();
    }
}
