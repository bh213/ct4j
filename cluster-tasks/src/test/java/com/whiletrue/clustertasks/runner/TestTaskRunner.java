package com.whiletrue.clustertasks.runner;

import com.whiletrue.clustertasks.scheduler.InternalTaskEvents;
import com.whiletrue.clustertasks.tasks.*;
import com.whiletrue.clustertasks.config.FixedTimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import com.whiletrue.clustertasks.tasks.ExampleTask;
import com.whiletrue.clustertasks.tasks.FailingTask;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("Test standard task runner")
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class TestTaskRunner {


    protected TaskPersistence taskPersistence;
    protected FixedTimeProvider fixedTimeProvider = new FixedTimeProvider();
    protected ClusterTasksConfig config;
    protected InternalTaskEvents internalEvents;

    @BeforeEach
    void init() {
        taskPersistence = Mockito.mock(TaskPersistence.class);
        internalEvents = Mockito.mock(InternalTaskEvents.class);
        config = new ClusterTasksConfigImpl();
        //when(clusterInstance.getInstanceId()).thenReturn("myclusterinstance");

    }


    @Test
    @DisplayName("Execute simple task")
    public void executeSimpleTask() throws Exception {
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


}

