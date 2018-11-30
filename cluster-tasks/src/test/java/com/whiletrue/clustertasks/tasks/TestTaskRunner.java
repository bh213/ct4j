package com.whiletrue.clustertasks.tasks;

import com.whiletrue.clustertasks.config.FixedTimeProvider;
import com.whiletrue.clustertasks.scheduler.InternalTaskEvents;
import com.whiletrue.clustertasks.tasks.recurring.RecurringSchedule;
import com.whiletrue.clustertasks.tasks.recurring.RecurringScheduleStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Collections;
import java.util.concurrent.Future;
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

        TaskWrapper<?> wrapper = new TaskWrapper<String>(task, "test", new TaskExecutionContext(0, "node1", "id-1","taskclass1", null ), Instant.now(), builder.build());

        final Future<TaskStatus> future = taskRunner.executeTask(wrapper, internalEvents);
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

        TaskWrapper<?> wrapper = new TaskWrapper<String>(task, "test", new TaskExecutionContext(0, "node1", "id-1","taskclass1", null ), Instant.now(), builder.build());

        final Future<TaskStatus> future = taskRunner.executeTask(wrapper, internalEvents);
        final TaskStatus taskStatus = future.get(1, TimeUnit.SECONDS);
        assertThat(taskStatus).isEqualTo(TaskStatus.Failure);

        verify(internalEvents, times(0)).taskCompleted(eq(wrapper), eq(0), anyFloat());
        verify(internalEvents, times(0)).taskFailed(eq(wrapper), eq(0));
        verify(internalEvents, times(1)).taskError(eq(wrapper), eq(0), anyFloat());
        verify(internalEvents, times(1)).taskStarted(eq(wrapper));
    }


    @DisplayName("Test task StdTaskRunner retry handling, non-recurring tasksk")
    @ParameterizedTest
    @CsvSource(
            { "5, 5, failure, false,, true, 0, ",    // retry # 5 out of 5 -> task failed
             "5, 3, retry, true, 4, false, 16000, ", // retry # 3 out of 5, resultingDelay 2000ms, backoff 2.0-> retry in 2^3 * 2000 ms
             "3, 0, retry, true, 1, false, 2000, ",   // retry # 0 out of 5, resultingDelay 2000ms, backoff 2.0-> retry in 2^0 * 2000 ms
             "3, 0, retry, true, 1, false, 2000, fixed:-:3000",   // retry # 0 out of 5, resultingDelay 2000ms, backoff 2.0-> retry in 2^0 * 2000 ms
            })
    public void testTaskRetry(int defaultRetry, int retryAttempt, String  expectedResult, boolean expectsPersistenceRetry,
                              Integer expectedPersistenceRetryNumber, boolean expectsPersistenceFailure, int resultingDelay
                              ) throws Exception {


        boolean expectTaskFailure = "failure".equals(expectedResult);
        final ClusterTasksConfigImpl config  = new ClusterTasksConfigImpl();
        config.setDefaultRetries(defaultRetry);
        config.setDefaultRetryDelay(2000);
        config.setDefaultRetryBackoffFactor(2);
        StdTaskRunner taskRunner = new StdTaskRunner(taskPersistence, config,  fixedTimeProvider);

        FailingTask task = new FailingTask();
        final TaskConfig.TaskConfigBuilder builder = new TaskConfig.TaskConfigBuilder(config, null, task.getClass());

        TaskWrapper<?> wrapper = new TaskWrapper<String>(task, "test", new TaskExecutionContext(retryAttempt, "node1", "id-1","taskclass1", null), fixedTimeProvider.getCurrent(),builder.build());


        taskRunner.handleRetry(wrapper, null, wrapper.getTaskExecutionContext(), internalEvents);

        verify(internalEvents, times(0)).taskCompleted(eq(wrapper), anyInt(), anyFloat());
        verify(internalEvents, times(expectTaskFailure ? 1 : 0)).taskFailed(eq(wrapper), eq(retryAttempt));
        verify(internalEvents, times(0)).taskError(eq(wrapper), anyInt(), anyFloat());
        verify(internalEvents, times(0)).taskStarted(eq(wrapper));

        if (expectsPersistenceRetry) {

            verify(taskPersistence, times(1)).unlockAndMarkForRetry(eq(wrapper), eq(expectedPersistenceRetryNumber), eq(fixedTimeProvider.getCurrent().plusMillis(resultingDelay)));
        }
        if (expectsPersistenceFailure) {
            verify(taskPersistence, times(1)).unlockAndChangeStatus(eq(Collections.singletonList(wrapper)), eq(TaskStatus.Failure));
        }
    }



    @DisplayName("Test task StdTaskRunner retry handling, recurring tasks")
    @ParameterizedTest
    @CsvSource(
            {

             "5, 5, failure, fail_and_retry, 0, 13000, 26000, fixed:-:13000",    // retry # 5 out of 5 -> task failed but will retry as it is recurring
             "10, 7, failure, fail_and_retry, 0,1000, 2000, fixed:-:1000",    // retry #7 out of 10 -> task failed as retry would be later than next scheduled tasks
              "5, 1, retry, retry, 2, 4000, 8000, fixed:-:10000",    // retry # 1 out of 5 -> normal retry
            })
    public void testRecurringTaskRetry(int maxRetries, int retryAttempt, String  expectedResult, String persistenceAction,
                              Integer expectedPersistenceRetryNumber, int nextRunMS, int nextScheduledMS,
                              String recurringScheduleInput) throws Exception {


        boolean expectsPersistenceRetry = "retry".equals(persistenceAction);
        boolean expectsPersistenceFailAndRetry= "fail_and_retry".equals(persistenceAction);

        boolean expectTaskFailure = "failure".equals(expectedResult);
        final ClusterTasksConfigImpl config  = new ClusterTasksConfigImpl();
        config.setDefaultRetries(maxRetries);
        config.setDefaultRetryDelay(2000);
        config.setDefaultRetryBackoffFactor(2);
        StdTaskRunner taskRunner = new StdTaskRunner(taskPersistence, config,  fixedTimeProvider);

        FailingTask task = new FailingTask();
        final TaskConfig.TaskConfigBuilder builder = new TaskConfig.TaskConfigBuilder(config, null, task.getClass());

        RecurringSchedule recurringSchedule = null;
        if (recurringScheduleInput != null) {
            recurringSchedule = RecurringSchedule.createNewRecurringSchedule(RecurringScheduleStrategy.fromString(recurringScheduleInput), fixedTimeProvider.getCurrent());
        }

        TaskWrapper<?> wrapper = new TaskWrapper<String>(task, "test", new TaskExecutionContext(retryAttempt, "node1", "id-1","taskclass1", recurringSchedule), fixedTimeProvider.getCurrent(),builder.build());


        taskRunner.handleRetry(wrapper, null, wrapper.getTaskExecutionContext(), internalEvents);

        verify(internalEvents, times(0)).taskCompleted(eq(wrapper), anyInt(), anyFloat());
        verify(internalEvents, times(expectTaskFailure ? 1 : 0)).taskFailed(eq(wrapper), eq(retryAttempt));
        verify(internalEvents, times(0)).taskError(eq(wrapper), anyInt(), anyFloat());
        verify(internalEvents, times(0)).taskStarted(eq(wrapper));

        if (expectsPersistenceRetry) {
            verify(taskPersistence, times(1)).unlockAndMarkForRetry(eq(wrapper), eq(expectedPersistenceRetryNumber), eq(fixedTimeProvider.getCurrent().plusMillis(nextRunMS)));
        }
        if (expectsPersistenceFailAndRetry) {
            verify(taskPersistence, times(1))
                    .unlockAndMarkForRetryAndSetScheduledNextRun(eq(wrapper), eq(expectedPersistenceRetryNumber), eq(fixedTimeProvider.getCurrent().plusMillis(nextRunMS)),
                            eq(fixedTimeProvider.getCurrent().plusMillis(nextScheduledMS)));
        }

        verify(taskPersistence, never()).unlockAndChangeStatus(eq(Collections.singletonList(wrapper)), eq(TaskStatus.Failure));

    }


}

