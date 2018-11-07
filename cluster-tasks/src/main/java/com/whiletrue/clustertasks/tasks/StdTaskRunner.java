package com.whiletrue.clustertasks.tasks;

import com.whiletrue.clustertasks.scheduler.InternalTaskEvents;
import com.whiletrue.clustertasks.timeprovider.TimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * StdTaskRunner runs Tasks on configured threadpool, tracks resource usage and handles exceptions.
 */
public class StdTaskRunner implements TaskRunner {

    private static Logger log = LoggerFactory.getLogger(StdTaskRunner.class);
    private final LinkedList<TaskWrapper<?>> currentlyExecutingTasksList;
    private final ExecutorService executorService;
    private final TaskPersistence taskPersistence;
    private final ClusterTasksConfig clusterTasksConfig;
    private final ResourceUsage currentResourceUsage;
    private final TimeProvider timeProvider;

    public StdTaskRunner(TaskPersistence taskPersistence, ClusterTasksConfig clusterTasksConfig, TimeProvider timeProvider) {
        this.taskPersistence = taskPersistence;
        this.clusterTasksConfig = clusterTasksConfig;
        this.executorService = Executors.newFixedThreadPool(clusterTasksConfig.getMaxNumberOfTasksPerNode() / 2);  // Threadpool TODO: add to config - size and type
        this.timeProvider = timeProvider;
        this.currentlyExecutingTasksList = new LinkedList<>();

        this.currentResourceUsage = new ResourceUsage(clusterTasksConfig.getConfiguredResources());

        log.info("Estimated configured resources: {}", this.currentResourceUsage); // TODO: move to initial banner?
    }


    @Override
    public <INPUT> CompletableFuture<TaskStatus> executeTask(TaskWrapper<INPUT> taskWrapper, InternalTaskEvents internalTaskEvents) {

        final Task<INPUT> task = taskWrapper.getTask();
        final INPUT input = taskWrapper.getInput();
        final TaskExecutionContext taskExecutionContext = taskWrapper.getTaskExecutionContext();

        log.debug("Running task with id '{}' and name '{}'", taskExecutionContext.getTaskId(), taskExecutionContext.getTaskName());


        synchronized (this) {
            currentlyExecutingTasksList.add(taskWrapper);
            currentResourceUsage.subtract(taskWrapper.getTaskConfig().getResourceUsage()); // task will now run so we decrease available resources
        }

        CompletableFuture<TaskStatus> future = new CompletableFuture<>();

        Future<?> taskExecution = executorService.submit(() -> {

            TaskConfig taskConfig = taskWrapper.getTaskConfig();

            taskExecutionContext.startTimer();
            try {
                internalTaskEvents.taskStarted(taskWrapper);
                // TODO: mark as running in DB?

                task.run(input, taskExecutionContext);

                final long duration = taskExecutionContext.stopTimer();
                internalTaskEvents.taskCompleted(taskWrapper, taskExecutionContext.getRetry(), duration / 1000000.0f);

                try {
                    task.onSuccess(input);
                } catch (Exception e) {
                    log.error("Task with id {} has throw exception in onSuccess: {}", taskExecutionContext.getTaskId(), e);
                }
                // TODO: use db aggregator for unlock and change status so we can do batch update?
                taskPersistence.unlockAndChangeStatus(Collections.singletonList(taskWrapper), TaskStatus.Success);
                log.info("Task id '{}', name '{}' was successful", taskExecutionContext.getTaskId(), taskExecutionContext.getTaskName());
                future.complete(TaskStatus.Success);
            } catch (Exception taskException) {
                log.error("Task with id {} has thrown exception: {}", taskExecutionContext.getTaskId(), taskException);

                final long duration = taskExecutionContext.stopTimer();
                internalTaskEvents.taskError(taskWrapper, taskExecutionContext.getRetry(), duration / 1000000.0f);


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

                future.complete(TaskStatus.Failure);
            }
        });
        future.handleAsync((taskStatus, throwable) -> {
            synchronized (this) {
                currentlyExecutingTasksList.remove(taskWrapper);
                currentResourceUsage.add(taskWrapper.getTaskConfig().getResourceUsage()); // task has completed, return resources to task runner
                return true;
            }
        });
        return future;
    }


    @Override
    public int getFreeTasksSlots() {
        return clusterTasksConfig.getMaxNumberOfTasksPerNode() - currentlyExecutingTasksList.size();
    }

    @Override
    public synchronized List<BasicTaskInfo> getOverdueTasks() {
        return this.currentlyExecutingTasksList.stream().filter(x -> x.getTaskExecutionContext().getRunningTimeInMs() > x.getTaskConfig().getMaxRunningTimeInMilliseconds()).map(BasicTaskInfo::new).collect(Collectors.toList());
    }

    @Override
    public synchronized List<BasicTaskInfo> getRunningTasks() {
        return this.currentlyExecutingTasksList.stream().map(BasicTaskInfo::new).collect(Collectors.toList());
    }

    @Override
    public synchronized ResourceUsage getCurrentResourcesAvailable() {
        return new ResourceUsage(currentResourceUsage);
    }

    <INPUT> void handleRetry(TaskWrapper<INPUT> taskWrapper, RetryPolicy retryPolicy, TaskExecutionContext taskExecutionContext, InternalTaskEvents internalTaskEvents) {

        final int maxRetries = retryPolicy == null ? clusterTasksConfig.getDefaultRetries() : retryPolicy.getMaxRetries();
        final int retryDelay = retryPolicy == null ? clusterTasksConfig.getDefaultRetryDelay() : retryPolicy.getRetryDelay();
        final float retryBackoffFactor = retryPolicy == null ? clusterTasksConfig.getDefaultRetryBackoffFactor() : retryPolicy.getRetryBackoffFactor();


        final int currentRetry = taskExecutionContext.getRetry();
        if (currentRetry == maxRetries) {
            log.info("Task {} has exhausted all retries. {} out of {}. Marking as failure.", taskExecutionContext.getTaskId(), currentRetry, maxRetries);
            taskPersistence.unlockAndChangeStatus(Collections.singletonList(taskWrapper), TaskStatus.Failure);
            try {
                taskWrapper.getTask().onFailure(taskWrapper.getInput());
                internalTaskEvents.taskFailed(taskWrapper, taskExecutionContext.getRetry());
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
