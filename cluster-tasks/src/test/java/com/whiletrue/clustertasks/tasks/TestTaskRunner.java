package com.whiletrue.clustertasks.tasks;

import com.whiletrue.clustertasks.config.FixedTimeProvider;
import com.whiletrue.clustertasks.scheduler.InternalTaskEvents;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("Test standard task runner")
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class TestTaskRunner {


    protected TaskPersistence taskPersistence;
    protected FixedTimeProvider fixedTimeProvider = new FixedTimeProvider();
    protected InternalTaskEvents internalEvents;

    @BeforeEach
    void init() {
        taskPersistence = Mockito.mock(TaskPersistence.class);
        internalEvents = Mockito.mock(InternalTaskEvents.class);

    }


    @Test
    @DisplayName("Execute simple task")
    public void executeSimpleTask() throws Exception {

        final ClusterTasksConfigImpl config  = new ClusterTasksConfigImpl();
        StdTaskRunner taskRunner = new StdTaskRunner(taskPersistence, config,  fixedTimeProvider);

        ExampleTask task = new ExampleTask();
        final TaskConfig.TaskConfigBuilder builder = new TaskConfig.TaskConfigBuilder(config, null, task.getClass());

        TaskWrapper<?> wrapper = new TaskWrapper<String>(task, "test", new TaskExecutionContext(0, "node1", "id-1","taskclass1" ), Instant.now(), builder.build());

        final CompletableFuture<TaskStatus> future = taskRunner.executeTask(wrapper, internalEvents);
        final TaskStatus taskStatus = future.get(1, TimeUnit.SECONDS);
        assertThat(taskStatus).isEqualTo(TaskStatus.Success);

        verify(internalEvents, times(1)).taskCompleted(eq(wrapper), eq(0), anyFloat());
        verify(internalEvents, times(0)).taskFailed(eq(wrapper), eq(0));
        verify(internalEvents, times(0)).taskError(eq(wrapper), eq(0), anyFloat());
        verify(internalEvents, times(1)).taskStarted(eq(wrapper));
    }


    @Test
    @DisplayName("Execute failing task")
    public void executeFailingTask() throws Exception {

        final ClusterTasksConfigImpl config  = new ClusterTasksConfigImpl();
        StdTaskRunner taskRunner = new StdTaskRunner(taskPersistence, config,  fixedTimeProvider);

        FailingTask task = new FailingTask();
        final TaskConfig.TaskConfigBuilder builder = new TaskConfig.TaskConfigBuilder(config, null, task.getClass());

        TaskWrapper<?> wrapper = new TaskWrapper<String>(task, "test", new TaskExecutionContext(0, "node1", "id-1","taskclass1" ), Instant.now(), builder.build());

        final CompletableFuture<TaskStatus> future = taskRunner.executeTask(wrapper, internalEvents);
        final TaskStatus taskStatus = future.get(1, TimeUnit.SECONDS);
        assertThat(taskStatus).isEqualTo(TaskStatus.Failure);

        verify(internalEvents, times(0)).taskCompleted(eq(wrapper), eq(0), anyFloat());
        verify(internalEvents, times(0)).taskFailed(eq(wrapper), eq(0));
        verify(internalEvents, times(1)).taskError(eq(wrapper), eq(0), anyFloat());
        verify(internalEvents, times(1)).taskStarted(eq(wrapper));
    }


    @DisplayName("Test task StdTaskRunner retry handling")
    @ParameterizedTest
    @CsvSource(
            { "5, 5, true, false, 0",
             "5, 3, false, true, 16000",
             "3, 0, false, true, 2000"
            })
    public void testTaskRetry(int defaultRetry, int retryAttempt, boolean failed, boolean persistenceRetry, int delay) throws Exception {

        final ClusterTasksConfigImpl config  = new ClusterTasksConfigImpl();
        config.setDefaultRetries(defaultRetry);
        config.setDefaultRetryDelay(2000);
        config.setDefaultRetryBackoffFactor(2);
        StdTaskRunner taskRunner = new StdTaskRunner(taskPersistence, config,  fixedTimeProvider);

        FailingTask task = new FailingTask();
        final TaskConfig.TaskConfigBuilder builder = new TaskConfig.TaskConfigBuilder(config, null, task.getClass());

        TaskWrapper<?> wrapper = new TaskWrapper<String>(task, "test", new TaskExecutionContext(retryAttempt, "node1", "id-1","taskclass1" ), Instant.now(), builder.build());


        taskRunner.handleRetry(wrapper, null, wrapper.getTaskExecutionContext(), internalEvents);

        verify(internalEvents, times(0)).taskCompleted(eq(wrapper), anyInt(), anyFloat());
        verify(internalEvents, times(failed ? 1 : 0)).taskFailed(eq(wrapper), eq(retryAttempt));
        verify(internalEvents, times(0)).taskError(eq(wrapper), anyInt(), anyFloat());
        verify(internalEvents, times(0)).taskStarted(eq(wrapper));

        if (persistenceRetry) {

            verify(taskPersistence, times(1)).unlockAndMarkForRetry(eq(wrapper), eq(retryAttempt+1), eq(fixedTimeProvider.getCurrent().plusMillis(delay)));
        } else {
            verify(taskPersistence, times(1)).unlockAndChangeStatus(eq(Collections.singletonList(wrapper)), eq(TaskStatus.Failure));
        }
    }


}

