package com.whiletrue.clustertasks.TaskManager;

import com.whiletrue.clustertasks.factory.TaskFactoryBase;
import com.whiletrue.clustertasks.inmemory.DefaultConstructorTaskFactory;
import com.whiletrue.clustertasks.inmemory.InMemoryTaskPersistence;
import com.whiletrue.clustertasks.instanceid.ClusterInstanceNaming;
import com.whiletrue.clustertasks.scheduler.Scheduler;
import com.whiletrue.clustertasks.tasks.*;
import com.whiletrue.clustertasks.config.FixedTimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import com.whiletrue.clustertasks.tasks.ExampleTask;
import com.whiletrue.clustertasks.tasks.IntegerTask;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("Scheduler tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestTaskManager {

    private TaskPersistence taskPersistence;
    private ClusterInstanceNaming clusterInstanceNaming;
    private TaskFactoryBase taskFactory;
    private FixedTimeProvider fixedTimeProvider = new FixedTimeProvider();
    private Scheduler scheduler;
    private TaskRunner taskRunner;
    private TaskManager taskManager;


    @BeforeEach
    void init() {
        clusterInstanceNaming = Mockito.mock(ClusterInstanceNaming.class);

        when(clusterInstanceNaming.getInstanceId()).thenReturn("myclusterinstance");

        taskFactory = new DefaultConstructorTaskFactory();
        fixedTimeProvider.setCurrent(Instant.now());
        final ClusterTasksConfigImpl clusterTasksConfig = new ClusterTasksConfigImpl();
        clusterTasksConfig.setMaximumPollingTimeMilliseconds(1);
        clusterTasksConfig.setMinimumPollingTimeMilliseconds(1);
        clusterTasksConfig.setDefaultRetryDelay(1);
        clusterTasksConfig.setDefaultRetries(3);
        clusterTasksConfig.setDefaultRetryBackoffFactor(1);


        taskPersistence = new InMemoryTaskPersistence(clusterInstanceNaming, taskFactory, clusterTasksConfig, fixedTimeProvider);
        taskRunner = new StdTaskRunner(taskPersistence, clusterTasksConfig, fixedTimeProvider);
        scheduler = new Scheduler(taskPersistence,taskRunner, clusterTasksConfig, fixedTimeProvider);
        taskManager = new StdTaskManager(taskPersistence, taskFactory, scheduler);
    }

    @Test
    @DisplayName("run single task with string input")
    public void testExampleTask() throws Exception {
        final String taskId = taskManager.queueTask(ExampleTask.class, "example input");
        taskManager.startScheduling();

        Thread.sleep(100);
        taskManager.stopScheduling();
        final TaskWrapper<?> task = taskPersistence.getTask(taskId); // TODO: use taskmanager
        assertThat(task).isNotNull();
        assertThat(taskPersistence.getTaskStatus(taskId))
                .isNotNull()
                .isEqualTo(TaskStatus.Success);

    }


    @Test
    @DisplayName("run single task with integer input")
    public void testIntegerTask() throws Exception {
        final String taskId = taskManager.queueTask(IntegerTask.class, 1111);
        taskManager.startScheduling();

        Thread.sleep(100);
        taskManager.stopScheduling();
        final TaskWrapper<?> task = taskPersistence.getTask(taskId); // TODO: use taskmanager
        assertThat(task).isNotNull();
        assertThat(taskPersistence.getTaskStatus(taskId))
                .isNotNull()
                .isEqualTo(TaskStatus.Success);

    }

    @Test
    @DisplayName("custom task factory adding and removing")
    public void testCustomFactory() throws Exception {
        for (int i = 0; i < 100; i++) {
            taskManager.setCallbacksListener(new TaskCallbacksListener() {});
            taskManager.setCallbacksListener(null);
        }
        taskManager.setCallbacksListener(new TaskCallbacksListener() {});
        assertThat(taskFactory.getCustomTaskFactoriesCount()).isEqualTo(1);
        taskManager.setCallbacksListener(null);
        assertThat(taskFactory.getCustomTaskFactoriesCount()).isEqualTo(0);
    }


    @Test
    @DisplayName("task events - task completed")
    public void testTaskCompletedEvent() throws Exception {
        final String taskId = taskManager.queueTask(IntegerTask.class, 1111);

        TaskCallbacksListener callbacks = Mockito.mock(TaskCallbacksListener.class);

        taskManager.setCallbacksListener(callbacks);
        taskManager.startScheduling();

        Thread.sleep(100);
        taskManager.stopScheduling();


        final TaskWrapper<?> task = taskPersistence.getTask(taskId); // TODO: use taskmanager
        assertThat(task).isNotNull();
        assertThat(taskPersistence.getTaskStatus(taskId))
                .isNotNull()
                .isEqualTo(TaskStatus.Success);

        verify(callbacks, times(1)).taskCompleted(any());
        verify(callbacks, times(0)).taskFailed(any());
        verify(callbacks, times(0)).taskOverdue(any());
        verify(callbacks, times(0)).taskCannotBeScheduled(any());
        verify(callbacks, atLeast(1)).createInstance(any());

    }


    @Test
    @DisplayName("task events - task failed")
    public void testTaskFailedEvent() throws Exception {
        final String taskId = taskManager.queueTask(FailingTask.class, "fail");

        TaskCallbacksListener callbacks = Mockito.mock(TaskCallbacksListener.class);

        taskManager.setCallbacksListener(callbacks);
        taskManager.startScheduling();

        for (int i = 0; i < 5; i++) { // move fixed time ahead or retry won't start
            Thread.sleep(50);
            fixedTimeProvider.plusMillis(50);
        }

        taskManager.stopScheduling();

        final TaskWrapper<?> task = taskPersistence.getTask(taskId); // TODO: use taskmanager
        assertThat(task).isNotNull();
        assertThat(taskPersistence.getTaskStatus(taskId))
                .isNotNull()
                .isEqualTo(TaskStatus.Failure);

        verify(callbacks, times(0)).taskCompleted(any());
        verify(callbacks, times(1)).taskFailed(any());
        verify(callbacks, times(0)).taskOverdue(any());
        verify(callbacks, times(0)).taskCannotBeScheduled(any());
        verify(callbacks, atLeast(1)).createInstance(any());

    }




}
