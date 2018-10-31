package com.whiletrue.clustertasks.scheduler;


import com.whiletrue.clustertasks.factory.TaskFactory;
import com.whiletrue.clustertasks.inmemory.DefaultConstructorTaskFactory;
import com.whiletrue.clustertasks.inmemory.InMemoryTaskPersistence;
import com.whiletrue.clustertasks.instanceid.ClusterInstance;
import com.whiletrue.clustertasks.tasks.*;
import com.whiletrue.clustertasks.config.FixedTimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import com.whiletrue.clustertasks.tasks.NoOpTestTask;

import java.time.Instant;
import java.util.ArrayList;

import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.mockito.Mockito.when;


@DisplayName("Scheduler tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestSchedulerIntegration {

    protected TaskPersistence taskPersistence;
    protected ClusterInstance clusterInstance;
    protected TaskFactory taskFactory;
    protected FixedTimeProvider fixedTimeProvider = new FixedTimeProvider();
    protected Scheduler scheduler;
    protected TaskRunner taskRunner;


    @BeforeEach
    void init() {
        clusterInstance = Mockito.mock(ClusterInstance.class);

        when(clusterInstance.getInstanceId()).thenReturn("myclusterinstance");

        taskFactory = new DefaultConstructorTaskFactory();
        fixedTimeProvider.setCurrent(Instant.now());
        final ClusterTasksConfig clusterTasksConfig = new ClusterTasksConfigImpl();
        taskPersistence = new InMemoryTaskPersistence(clusterInstance, taskFactory, clusterTasksConfig, fixedTimeProvider);
        taskRunner = new StdTaskRunner(taskPersistence, clusterTasksConfig, fixedTimeProvider);
        scheduler = new Scheduler(taskPersistence, taskRunner, clusterTasksConfig, fixedTimeProvider);
    }

    @Test
    @DisplayName("test single task")
    public void testSingleTask() throws Exception {
        final String taskId = taskPersistence.queueTask(new NoOpTestTask(), "very valid string");
        scheduler.startScheduling();
        Thread.sleep(100);
        scheduler.stopScheduling();
        final TaskWrapper<?> task = taskPersistence.getTask(taskId);
        assertThat(task).isNotNull();
        assertThat(taskPersistence.getTaskStatus(taskId))
                .isNotNull()
                .isEqualTo(TaskStatus.Success);

    }

    @Test
    @DisplayName("start scheduler multiple times")
    public void startSchedulerMultipleTimes() throws Exception {
        scheduler.startScheduling();
        Thread.sleep(100);
        scheduler.startScheduling();
        scheduler.startScheduling();
        scheduler.stopScheduling();

    }

    @Test
    @DisplayName("stop scheduler multiple times")
    public void stopSchedulerMultipleTimes() throws Exception {
        scheduler.startScheduling();

        scheduler.stopScheduling();
        scheduler.stopScheduling();
        Thread.sleep(100);
        scheduler.stopScheduling();
    }


    @Test
    @DisplayName("test multiple tasks")
    public void testMultipleTasksTask() throws Exception {
        ArrayList<String> tasks = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            final String taskId = taskPersistence.queueTask(new NoOpTestTask(), "very valid string");
            tasks.add(taskId);
        }

        scheduler.startScheduling();

        assertTimeout(ofSeconds(3), () -> {
                    while (taskPersistence.countPendingTasks() > 0) {
                        Thread.sleep(100);
                    }
                });
        scheduler.stopScheduling();

        for (String taskId : tasks) {
            final TaskWrapper<?> task = taskPersistence.getTask(taskId);
            assertThat(task).isNotNull();

            assertThat(taskPersistence.getTaskStatus(taskId))
                    .isNotNull()
                    .isEqualTo(TaskStatus.Success);
        }
    }
}