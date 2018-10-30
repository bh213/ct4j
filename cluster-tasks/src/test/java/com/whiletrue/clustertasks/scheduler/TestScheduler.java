package com.whiletrue.clustertasks.scheduler;


import com.whiletrue.clustertasks.config.FixedTimeProvider;
import com.whiletrue.clustertasks.factory.TaskFactory;
import com.whiletrue.clustertasks.instanceid.ClusterInstance;
import com.whiletrue.clustertasks.tasks.*;
import com.whiletrue.clustertasks.testutils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;


@DisplayName("Scheduler tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestScheduler {

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

        taskFactory = Mockito.mock(TaskFactory.class);
        fixedTimeProvider.setCurrent(Instant.now());
        taskPersistence = Mockito.mock(TaskPersistence.class);
        taskRunner = Mockito.mock(TaskRunner.class);

    }

    @Test
    @DisplayName("test resource estimate")
    public void testFreeTasksSlots() {
        ClusterTasksConfig clusterTasksConfig = new ClusterTasksConfigImpl();
        scheduler = new Scheduler(taskPersistence, taskRunner, clusterTasksConfig, fixedTimeProvider);
        final ResourceUsage resourceUsage = new ResourceUsage(77, 78);
        when(taskRunner.getCurrentResourcesAvailable()).thenReturn(resourceUsage);
        assertThat(scheduler.getFreeResourcesEstimate()).isEqualTo(resourceUsage);
        assertThat(scheduler.getFreeResourcesEstimate().getCpuCoreUsage()).isEqualTo(77);
        assertThat(scheduler.getFreeResourcesEstimate().getMaximumMemoryUsageInMb()).isEqualTo(78);
    }



    @DisplayName("test scheduler schedulerFitAsManyTaskAsPossible")
    @ParameterizedTest
    @ValueSource(strings= {"true","false"})
    public void testSchedulerFitAsManyTaskAsPossible(boolean schedulerFitAsManyTaskAsPossibleEnabled) throws Exception {

        ClusterTasksConfigImpl clusterTasksConfig = new ClusterTasksConfigImpl();
        clusterTasksConfig.setSchedulerFitAsManyTaskAsPossible(schedulerFitAsManyTaskAsPossibleEnabled);

        when(taskRunner.getCurrentResourcesAvailable()).thenReturn(new ResourceUsage(11, 1111));

        List<TaskWrapper<?>> tasks = new ArrayList<>();
        tasks.addAll(IntStream.range(0, 165).mapToObj(
                x -> TestUtils.createTaskWrapper(
                        new ExampleTask(), "test", 0, "node1", Integer.toString(x + 1), "example task", clusterTasksConfig)).collect(Collectors.toList()));

        tasks.add(3, TestUtils.createTaskWrapper(new ExampleTask(), "test", 0, "node1", "1", "too large task",
                clusterTasksConfig, taskConfigBuilder -> taskConfigBuilder.estimateResourceUsage(1000, 100000)));

        when(taskPersistence.pollForNextTasks(166)).thenReturn(tasks);

        scheduler = new Scheduler(taskPersistence, taskRunner, clusterTasksConfig, fixedTimeProvider);

        scheduler.pollForTasks(166);

        verify(taskPersistence, times(1)).tryClaimTasks(argThat(list -> list.size() == (schedulerFitAsManyTaskAsPossibleEnabled ? 165 : 3)));
    }


    @DisplayName("test scheduler schedulerPollAfterTaskCompletion")
    @ParameterizedTest
    @ValueSource(strings= {"true","false"})
    public void testSchedulerPollAfterTaskCompletion(boolean schedulerPollAfterTaskCompletionEnabled) throws Exception {

        ClusterTasksConfigImpl clusterTasksConfig = new ClusterTasksConfigImpl();
        clusterTasksConfig.setSchedulerPollAfterTaskCompletion(schedulerPollAfterTaskCompletionEnabled);

        when(taskRunner.getCurrentResourcesAvailable()).thenReturn(new ResourceUsage(11, 1111));


        final TaskWrapper<String> taskWrapper = TestUtils.createTaskWrapper(new ExampleTask(), "test", 0, "node1", "1", "too large task",
                clusterTasksConfig, taskConfigBuilder -> taskConfigBuilder.estimateResourceUsage(0, 0));

        when(taskPersistence.pollForNextTasks(100)).thenReturn(Collections.singletonList(taskWrapper));

        scheduler = new Scheduler(taskPersistence, taskRunner, clusterTasksConfig, fixedTimeProvider);
        scheduler.pollForTasks(166);

        scheduler.taskCompleted(taskWrapper, 0, 1000);
        if (schedulerPollAfterTaskCompletionEnabled) assertThat(scheduler.getNextPollingTime()).isEqualTo(fixedTimeProvider.getCurrent().plusMillis(clusterTasksConfig.getMinimumPollingTimeMilliseconds()));
        else assertThat(scheduler.getNextPollingTime()).isNull();

    }






}
