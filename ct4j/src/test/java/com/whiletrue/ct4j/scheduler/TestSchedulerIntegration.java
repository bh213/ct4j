package com.whiletrue.ct4j.scheduler;


import com.whiletrue.ct4j.config.FixedTimeProvider;
import com.whiletrue.ct4j.factory.TaskFactory;
import com.whiletrue.ct4j.inmemory.DefaultConstructorTaskFactory;
import com.whiletrue.ct4j.inmemory.InMemoryTaskPersistence;
import com.whiletrue.ct4j.instanceid.ClusterInstanceNaming;
import com.whiletrue.ct4j.tasks.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

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

    @ParameterizedTest
    @DisplayName("scheduler runs large number of tasks")
    @EnumSource(ScheduledTaskAction.class)
    public void testScheduledTasks(ScheduledTaskAction action) throws Exception {

        var counter = new AtomicInteger();
        scheduler.startScheduling();

        for (int i = 0; i < 100; i++) {
            final String taskId = taskPersistence.registerScheduledTask(new AtomicCounterTask(), counter, 1, action);
            fixedTimeProvider.plusMillis(1);
            Thread.sleep(5);
        }
        scheduler.stopScheduling();
        assertThat(counter.get()).isGreaterThan(1); // check for at least single execution


    }

}
