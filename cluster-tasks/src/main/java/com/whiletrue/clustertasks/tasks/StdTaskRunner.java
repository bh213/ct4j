package com.whiletrue.clustertasks.tasks;

import com.whiletrue.clustertasks.scheduler.InternalTaskEvents;
import com.whiletrue.clustertasks.timeprovider.TimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.Collections;
import java.util.concurrent.*;

public class StdTaskRunner implements TaskRunner {

    private static Logger log = LoggerFactory.getLogger(StdTaskRunner.class);
    private final CopyOnWriteArrayList<Task<?>> tasksList; // TODO:
    private final ExecutorService executorService;
    private final TaskPersistence taskPersistence;
    private final ClusterTasksConfig clusterTasksConfig;
    private final ResourceUsage currentResourceUsage;
    private final TimeProvider timeProvider;

    public StdTaskRunner(TaskPersistence taskPersistence, ClusterTasksConfig clusterTasksConfig, TimeProvider timeProvider) {
        this.taskPersistence = taskPersistence;
        this.clusterTasksConfig = clusterTasksConfig;
        this.executorService = Executors.newFixedThreadPool(clusterTasksConfig.getMaxNumberOfTasksPerNode()/2);  // Threadpool TODO
        this.timeProvider = timeProvider;
        this.tasksList = new CopyOnWriteArrayList<>();

        long allocatedMemory      = (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory());
        long presumableFreeMemory = Runtime.getRuntime().maxMemory() - allocatedMemory;

        this.currentResourceUsage = new ResourceUsage( Runtime.getRuntime().availableProcessors(), presumableFreeMemory/1000000.0f);
        log.info("estimated available resources: {}", this.currentResourceUsage);
    }

    @Override
    public <INPUT> void executeTask(TaskWrapper<INPUT> taskWrapper, InternalTaskEvents internalTaskEvents){

        final Task<INPUT> task = taskWrapper.getTask();
        final INPUT input = taskWrapper.getInput();
        final TaskExecutionContext taskExecutionContext = taskWrapper.getTaskExecutionContext();

        log.debug("Running task with id '{}' and name '{}'", taskExecutionContext.getTaskId(), taskExecutionContext.getTaskName());

        tasksList.add(task);
        synchronized (this) {
            currentResourceUsage.subtract(taskWrapper.getTaskConfig().getResourceUsage());
        }

        CompletableFuture<TaskStatus> future = new CompletableFuture<>();

        Future<?> taskExecution = executorService.submit(() -> {

            TaskConfig taskConfig = taskWrapper.getTaskConfig();
            final long start = System.nanoTime();
            try {
                internalTaskEvents.taskStarted(task.getClass(), taskExecutionContext.getTaskId());
                // TODO: mark as running in DB

                task.run(input, taskExecutionContext);

                final long duration = System.nanoTime() - start;
                internalTaskEvents.taskCompleted(task.getClass(), taskExecutionContext.getTaskId(), taskExecutionContext.getRetry(), duration / 1000.0f);
                synchronized (this) {
                    currentResourceUsage.add(taskConfig.getResourceUsage());
                }


                try {
                    task.onSuccess(input);
                } catch (Exception e) {
                    log.error("Task with id {} has throw exception in onSuccess: {}", taskExecutionContext.getTaskId(), e);
                }
                // TODO: add aggregator
                taskPersistence.unlockAndChangeStatus(Collections.singletonList(taskWrapper), TaskStatus.Success);
                log.info("Task id '{}', name '{}' was successful", taskExecutionContext.getTaskId(), taskExecutionContext.getTaskName());
                future.complete(TaskStatus.Success);
            } catch (Exception taskException) {
                log.error("Task with id {} has thrown exception: {}", taskExecutionContext.getTaskId(), taskException);
                final long duration = System.nanoTime() - start;
                internalTaskEvents.taskError(task.getClass(), taskExecutionContext.getTaskId(), taskExecutionContext.getRetry(), duration / 1000.0f);

                synchronized (this) {
                    currentResourceUsage.add(taskConfig.getResourceUsage());
                }


                RetryPolicy newRetryPolicy = null;
                try {
                    newRetryPolicy = task.onError(input, taskException, taskConfig.getRetryPolicy());
                } catch (Exception onError) {
                    log.error("Error in task {} onError handler: {}", taskExecutionContext.getTaskId(), onError);
                }
                try {
                    handleRetry(taskWrapper, newRetryPolicy == null ? taskConfig.getRetryPolicy() : newRetryPolicy, taskExecutionContext, internalTaskEvents);
                } catch (Exception e) {
                    log.error("Error handling retry for task {}:{}", taskExecutionContext.getTaskId(), e);
                    taskPersistence.unlockAndChangeStatus(Collections.singletonList(taskWrapper), TaskStatus.Failure);
                }

                future.complete(TaskStatus.Failure); // TODO
            }
        });
        future.handleAsync((taskStatus, throwable) -> tasksList.remove(task));
    }


    @Override
    public int getFreeTasksSlots() {
        return clusterTasksConfig.getMaxNumberOfTasksPerNode() - tasksList.size();
    }


    @Override
    public synchronized ResourceUsage getCurrentResourcesAvailable() {
        return new ResourceUsage(currentResourceUsage);
    }

    private <INPUT> void handleRetry(TaskWrapper<INPUT> taskWrapper, RetryPolicy retryPolicy, TaskExecutionContext taskExecutionContext, InternalTaskEvents internalTaskEvents) {

        final int maxRetries = retryPolicy == null ? clusterTasksConfig.getDefaultRetries() : retryPolicy.getMaxRetries();
        final int retryDelay = retryPolicy == null ? clusterTasksConfig.getDefaultRetryDelay() : retryPolicy.getRetryDelay();
        final float retryBackoffFactor = retryPolicy == null ? clusterTasksConfig.getDefaultRetryBackoffFactor() : retryPolicy.getRetryBackoffFactor();


        final int currentRetry = taskExecutionContext.getRetry();
        if (currentRetry == maxRetries)
        {
            log.info("Task {} has exhausted all retries. {} out of {}. Marking as failure.", taskExecutionContext.getTaskId(), currentRetry, maxRetries);
            taskPersistence.unlockAndChangeStatus(Collections.singletonList(taskWrapper), TaskStatus.Failure);
            try {
                taskWrapper.getTask().onFailure(taskWrapper.getInput());
                internalTaskEvents.taskFailed(taskWrapper.getTask().getClass(), taskExecutionContext.getTaskId(), taskExecutionContext.getRetry());
            } catch (Exception e) {
                log.error("Error in task {} onFailure handler: {}", taskExecutionContext.getTaskId(), e);
            }

        } else {
            final double calculatedDelay = retryDelay * Math.pow(retryBackoffFactor, currentRetry);
            log.info("Failed task {} scheduled for retry {} out of {}, delay {}", taskExecutionContext.getTaskId(), currentRetry + 1, maxRetries, calculatedDelay);
            taskPersistence.unlockAndMarkForRetry(taskWrapper, currentRetry + 1, timeProvider.getCurrent().plusMillis((long) calculatedDelay));

        }
    }





}
