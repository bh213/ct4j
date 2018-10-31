package com.whiletrue.clustertasks.scheduler;


import com.whiletrue.clustertasks.factory.TaskFactory;
import com.whiletrue.clustertasks.inmemory.DefaultConstructorTaskFactory;
import com.whiletrue.clustertasks.inmemory.InMemoryTaskPersistence;
import com.whiletrue.clustertasks.instanceid.ClusterInstanceNaming;
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

    private TaskPersistence taskPersistence;
    private ClusterInstanceNaming clusterInstanceNaming;
    private FixedTimeProvider fixedTimeProvider = new FixedTimeProvider();
    private Scheduler scheduler;

    @BeforeEach
    void init() {
        clusterInstanceNaming = Mockito.mock(ClusterInstanceNaming.class);

        when(clusterInstanceNaming.getInstanceId()).thenReturn("myclusterinstance");

        TaskFactory taskFactory = new DefaultConstructorTaskFactory();
        fixedTimeProvider.setCurrent(Instant.now());
        final ClusterTasksConfigImpl clusterTasksConfig = new ClusterTasksConfigImpl();
        clusterTasksConfig.setMaximumPollingTimeMilliseconds(100);
        clusterTasksConfig.setMinimumPollingTimeMilliseconds(1);
                taskPersistence = new InMemoryTaskPersistence(clusterInstanceNaming, taskFactory, clusterTasksConfig, fixedTimeProvider);
        TaskRunner taskRunner = new StdTaskRunner(taskPersistence, clusterTasksConfig, fixedTimeProvider);
        scheduler = new Scheduler(taskPersistence, taskRunner, clusterTasksConfig, fixedTimeProvider);
    }

    @Test
    @DisplayName("test single task execution")
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
    @DisplayName("startScheduling() called multiple times with no side effects")
    public void startSchedulerMultipleTimes() throws Exception {
        scheduler.startScheduling();
        Thread.sleep(100);
        scheduler.startScheduling();
        scheduler.startScheduling();
        scheduler.stopScheduling();
    }

    @Test
    @DisplayName("stopScheduler() called multiple times with no side effects")
    public void stopSchedulerMultipleTimes() throws Exception {
        scheduler.startScheduling();

        scheduler.stopScheduling();
        scheduler.stopScheduling();
        Thread.sleep(100);
        scheduler.stopScheduling();
    }


    @Test
    @DisplayName("scheduler runs large number of tasks")
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
