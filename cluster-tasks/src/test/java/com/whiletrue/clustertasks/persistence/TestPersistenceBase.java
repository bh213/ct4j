package com.whiletrue.clustertasks.persistence;


import com.whiletrue.clustertasks.factory.TaskFactory;
import com.whiletrue.clustertasks.instanceid.ClusterInstanceNaming;
import com.whiletrue.clustertasks.tasks.*;
import com.whiletrue.clustertasks.config.FixedTimeProvider;
import com.whiletrue.clustertasks.tasks.recurring.RecurringSchedule;
import com.whiletrue.clustertasks.tasks.recurring.RecurringScheduleStrategy;
import com.whiletrue.clustertasks.tasks.recurring.RecurringScheduleStrategyFixed;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import com.whiletrue.clustertasks.tasks.NoOpTestTask;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public abstract class TestPersistenceBase {
    protected TaskPersistence taskPersistence;
    protected ClusterInstanceNaming clusterInstanceNaming;
    protected TaskFactory taskFactory;
    protected FixedTimeProvider fixedTimeProvider = new FixedTimeProvider();

    @Test
    @DisplayName("Test simple task creation, null input")
    public void createTaskNullInput() throws Exception {
        taskPersistence.queueTask(new NoOpTestTask(), null);
    }

    @Test
    @DisplayName("Test simple task creation, valid string")
    public void createTaskValidInput() throws Exception {
        taskPersistence.queueTask(new NoOpTestTask(), "very valid string");
    }

    @Test
    @DisplayName("task create, get, delete")
    public void createGetDelete() throws Exception {
        final String taskId = taskPersistence.queueTask(new NoOpTestTask(), "very valid string");
        assertThat(taskId).isNotNull();
        final TaskWrapper<?> getTask = taskPersistence.getTask(taskId);
        assertThat(getTask).isNotNull();
        assertThat(getTask.getInput())
                .isNotNull()
                .isEqualTo("very valid string");
        assertThat(getTask.getTaskExecutionContext()).isNotNull();

        assertThat(getTask.getTaskExecutionContext().getTaskId())
                .isNotNull()
                .isEqualTo(taskId);
        taskPersistence.deleteTask(taskId);

        assertThat(taskPersistence.getTask(taskId)).isNull();
    }


    @Test
    @DisplayName("Basic polling test")
    public void basicPollingTest() throws Exception {
        final String taskId = taskPersistence.queueTask(new NoOpTestTask(), "very valid string");
        final List<TaskWrapper<?>> tasks = taskPersistence.pollForNextTasks(100);
        assertThat(tasks)
                .isNotNull()
                .hasSize(1)
                .extracting(input -> input.getTaskExecutionContext().getTaskId())
                .containsExactly(taskId);

        final List<TaskWrapper<?>> tasks2 = taskPersistence.pollForNextTasks(100);
        assertThat(tasks2)
                .as("should return the same result next time")
                .isNotNull()
                .usingRecursiveFieldByFieldElementComparator()
                .isEqualTo(tasks);
    }

    @Test
    @DisplayName("claiming workflow")
    public void testClaimingWorkflow() throws Exception {
        final String taskId = taskPersistence.queueTask(new NoOpTestTask(), "very valid string");
        final List<TaskWrapper<?>> tasks = taskPersistence.pollForNextTasks(100);
        assertThat(tasks)
                .isNotNull()
                .hasSize(1)
                .extracting(input -> input.getTaskExecutionContext().getTaskId())
                .containsExactly(taskId);

        final int claimed = taskPersistence.tryClaimTasks(tasks);
        assertThat(claimed).isEqualTo(1);
        final List<TaskWrapper<?>> claimedTasks = taskPersistence.findClaimedTasks(tasks);
        assertThat(claimedTasks)
                .isNotNull()
                .hasSize(1)
                .hasOnlyOneElementSatisfying(x -> assertThat(x.getTaskExecutionContext().getTaskId()).isEqualTo(taskId));


        final int claimed2 = taskPersistence.tryClaimTasks(tasks);
        assertThat(claimed2).isEqualTo(0);
    }

    @Test
    @DisplayName("tasks should be claimed highest priority first")
    public void tasksShouldBeClaimedHighestPriorityFirst() throws Exception {
        final String taskId1 = taskPersistence.queueTask(new NoOpTestTask(), "input 1", 3000);
        final String taskId2 = taskPersistence.queueTask(new NoOpTestTask(), "input 2", 2000);
        final String taskId3 = taskPersistence.queueTask(new NoOpTestTask(), "input 3", 1000);
        final String taskId4 = taskPersistence.queueTask(new NoOpTestTask(), "input 4", 4000);

        final List<String> ids = Arrays.asList(taskId4, taskId1, taskId2, taskId3);

        final List<TaskWrapper<?>> tasks = taskPersistence.pollForNextTasks(100);
        assertThat(tasks)
                .isNotNull()
                .hasSize(4)
                .extracting(input -> input.getTaskExecutionContext().getTaskId())
                .containsExactly(taskId4, taskId1, taskId2, taskId3);

        for (int i = 0; i < 4; i++) {
            final List<TaskWrapper<?>> taskToClaim = tasks.subList(i, i + 1);
            final int claimed = taskPersistence.tryClaimTasks(taskToClaim);
            assertThat(claimed).isEqualTo(1);
            final List<TaskWrapper<?>> claimedTasks = taskPersistence.findClaimedTasks(taskToClaim);
            assertThat(claimedTasks)
                    .isNotNull()
                    .hasSize(1);
            assertThat(claimedTasks.get(0).getTaskExecutionContext().getTaskId()).isEqualTo(ids.get(i));
        }

        final int claimed2 = taskPersistence.tryClaimTasks(tasks);
        assertThat(claimed2).isEqualTo(0);
    }

    @Test
    @DisplayName("unlock and change status to Failure should make test ineligible for polling")
    public void failedTaskShouldNotBePollable() throws Exception {
        final String taskId = taskPersistence.queueTask(new NoOpTestTask(), "very valid string");

        List<TaskWrapper<?>> tasks = taskPersistence.pollForNextTasks(100);
        assertThat(tasks)
                .isNotNull()
                .hasSize(1)
                .extracting(input -> input.getTaskExecutionContext().getTaskId())
                .containsExactly(taskId);

        TaskWrapper<?> task = taskPersistence.getTask(taskId);
        assertThat(task).isNotNull();

        final int claimed = taskPersistence.tryClaimTasks(Collections.singletonList(task));
        assertThat(claimed).isEqualTo(1);
        taskPersistence.unlockAndChangeStatus(Collections.singletonList(task), TaskStatus.Failure);

        tasks = taskPersistence.pollForNextTasks(100);
        assertThat(tasks)
                .isNotNull()
                .hasSize(0);

        task = taskPersistence.getTask(taskId);
        assertThat(task).isNotNull();
        assertThat(task.getTaskExecutionContext().getTaskId()).isEqualTo(taskId);
    }

    @Test
    @DisplayName("unlock and retry should make test ineligible for polling until retry delay expires")
    public void retriedTaskShouldNotBePollableUntilTimeout() throws Exception {
        final String taskId = taskPersistence.queueTask(new NoOpTestTask(), "very valid string");

        List<TaskWrapper<?>> tasks = taskPersistence.pollForNextTasks(100);
        assertThat(tasks)
                .isNotNull()
                .hasOnlyOneElementSatisfying(x -> assertThat(x.getTaskExecutionContext().getTaskId()).isEqualTo(taskId));

        TaskWrapper<?> task = taskPersistence.getTask(taskId);
        assertThat(task).isNotNull();
        assertThat(task.getTaskExecutionContext().getTaskId()).isEqualTo(taskId);

        final int claimed = taskPersistence.tryClaimTasks(Collections.singletonList(task));
        assertThat(claimed).isEqualTo(1);

        taskPersistence.unlockAndMarkForRetry(task, 1, fixedTimeProvider.getCurrent().plusMillis(3210));

        tasks = taskPersistence.pollForNextTasks(100);
        assertThat(tasks)
                .isNotNull()
                .hasSize(0);

        task = taskPersistence.getTask(taskId);
        assertThat(task).isNotNull();
        assertThat(task.getTaskExecutionContext().getTaskId()).isEqualTo(taskId);

        fixedTimeProvider.plusMillis(3200);

        tasks = taskPersistence.pollForNextTasks(100);
        assertThat(tasks)
                .isNotNull()
                .hasSize(0);

        task = taskPersistence.getTask(taskId);
        assertThat(task).isNotNull();
        assertThat(task.getTaskExecutionContext().getTaskId()).isEqualTo(taskId);


        fixedTimeProvider.plusMillis(50);

        tasks = taskPersistence.pollForNextTasks(100);
        assertThat(tasks)
                .isNotNull()
                .hasOnlyOneElementSatisfying(x -> assertThat(x.getTaskExecutionContext().getTaskId()).isEqualTo(taskId));
    }


    @Test
    @DisplayName("delete task test")
    public void testDeleteTask() throws Exception {
        final String id = taskPersistence.queueTask(new NoOpTestTask(), null);
        final TaskWrapper<?> task = taskPersistence.getTask(id);
        assertThat(task).isNotNull();
        assertThat(task.getTaskExecutionContext().getTaskId()).isEqualTo(id);
        taskPersistence.deleteTask(id);
        final TaskWrapper<?> deletedTask = taskPersistence.getTask(id);
        assertThat(deletedTask).isNull();

    }





    @ParameterizedTest
    @DisplayName("basic create recurring task")
    @EnumSource(ScheduledTaskAction.class)
    public void testRecurringTask(ScheduledTaskAction taskAction) throws Exception {
        final String id = taskPersistence.registerScheduledTask(new NoOpTestTask(), null, 3211, taskAction);
        final TaskWrapper<?> task = taskPersistence.getTask(id);
        assertThat(task).isNotNull();
        assertThat(task.getTaskExecutionContext().getTaskId()).isEqualTo(id);
        assertThat(task.getTaskExecutionContext().getRecurringSchedule()).isPresent();
        final RecurringSchedule recurringSchedule = task.getTaskExecutionContext().getRecurringSchedule().get();
        assertThat(recurringSchedule.getStrategy())
                .isNotNull()
                .isInstanceOf(RecurringScheduleStrategyFixed.class);

        assertThat(recurringSchedule.getStrategy().toDatabaseString()).isEqualTo(RecurringScheduleStrategy.FIXED + RecurringScheduleStrategy.SEPARATOR + "3211");
    }

    @ParameterizedTest
    @DisplayName("recurring task replace tests")
    @EnumSource(ScheduledTaskAction.class)
    public void testRecurringTaskReplace(ScheduledTaskAction taskAction) throws Exception {
        final String id = taskPersistence.registerScheduledTask(new NoOpTestTask(), null, 2999, taskAction);
        final TaskWrapper<?> task = taskPersistence.getTask(id);
        assertThat(task).isNotNull();
        assertThat(task.getTaskExecutionContext().getTaskId()).isEqualTo(id);

        final String newId = taskPersistence.registerScheduledTask(new NoOpTestTask(), null, 3666, taskAction);
        assertThat(newId).isNotNull();
        final TaskWrapper<?> newTask = taskPersistence.getTask(newId);
        assertThat(newTask).isNotNull();

        if (taskAction == ScheduledTaskAction.AlwaysAdd) {
            assertThat(id).as("check new task was created").isNotEqualTo(newId);
            assertThat(taskPersistence.getTask(id)).isNotNull();
            assertThat(taskPersistence.getTask(newId)).isNotNull();


        }
        else if (taskAction.isReplaceTasks()) {
            assertThat(id).as("check new task was created").isNotEqualTo(newId);
            assertThat(taskPersistence.getTask(id)).isNull();
            assertThat(taskPersistence.getTask(newId)).isNotNull();


        } else {
            assertThat(id).as("check task was updated").isEqualTo(newId);
            assertThat(taskPersistence.getTask(newId)).isNotNull();
        }

        assertThat(newTask.getTaskExecutionContext()).isNotNull();
        assertThat(newTask.getTaskExecutionContext().getRecurringSchedule())
                .isPresent();
        assertThat(newTask.getTaskExecutionContext().getRecurringSchedule().get().getStrategy())
                .isInstanceOf(RecurringScheduleStrategyFixed.class)
                .isEqualToComparingFieldByField(new RecurringScheduleStrategyFixed(3666));
    }




    @ParameterizedTest
    @CsvSource(value = {
            "AlwaysAdd, false, false, false, false, 0",
            "SingletonTaskReplace, true, true, true, true, 0",
            "SingletonTaskKeepExisting, false, false, false, false, 0",
            "TaskPerInputReplace, false, false, true, true, 0",
            "TaskPerInputKeepExisting, false, false, false, false, 0"
    })
    @DisplayName("scheduled task replacement test")
    public void testSingletonTaskKeepExisting(ScheduledTaskAction scheduledTaskAction, boolean task1Null, boolean task2Null, boolean task3Null, boolean task4Null, int newIdEqualsTo) throws Exception {
        final String id1 = taskPersistence.registerScheduledTask(new NoOpTestTask(), "buga", 2999, ScheduledTaskAction.AlwaysAdd);
        final String id2 = taskPersistence.registerScheduledTask(new NoOpTestTask(), "buga", 2999, ScheduledTaskAction.AlwaysAdd);
        final String id3 = taskPersistence.registerScheduledTask(new NoOpTestTask(), null, 2999, ScheduledTaskAction.AlwaysAdd);
        final String id4 = taskPersistence.registerScheduledTask(new NoOpTestTask(), null, 2999, ScheduledTaskAction.AlwaysAdd);

        assertThat(List.of(id1, id2, id3, id4).stream().distinct()).as("Ids are unique").hasSize(4);
        assertThat(List.of(taskPersistence.getTask(id1), taskPersistence.getTask(id2),
                taskPersistence.getTask(id3), taskPersistence.getTask(id4)).stream().distinct()).as("Task are unique").hasSize(4);

        final String newId = taskPersistence.registerScheduledTask(new NoOpTestTask(), null, 3777,scheduledTaskAction);

        assertThat(taskPersistence.getTask(id1) == null).isEqualTo(task1Null);
        assertThat(taskPersistence.getTask(id2) == null).isEqualTo(task2Null);
        assertThat(taskPersistence.getTask(id3) == null).isEqualTo(task3Null);
        assertThat(taskPersistence.getTask(id4) == null).isEqualTo(task4Null);

        switch(newIdEqualsTo) {
            case 1: assertThat(newId).isEqualTo(id1); break;
            case 2: assertThat(newId).isEqualTo(id2); break;
            case 3: assertThat(newId).isEqualTo(id3); break;
            case 4: assertThat(newId).isEqualTo(id4); break;
            default: assertThat(List.of(id1, id2, id3, id4)).doesNotContain(newId);
        }
    }



}

