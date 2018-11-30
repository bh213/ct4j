package com.whiletrue.clustertasks.persistence;


import com.whiletrue.clustertasks.factory.TaskFactory;
import com.whiletrue.clustertasks.inmemory.InMemoryTaskPersistence;
import com.whiletrue.clustertasks.instanceid.ClusterInstanceNaming;
import com.whiletrue.clustertasks.tasks.ClusterTasksConfigImpl;
import com.whiletrue.clustertasks.tasks.NoOpTestTask;
import com.whiletrue.clustertasks.tasks.ScheduledTaskAction;
import com.whiletrue.clustertasks.tasks.TaskWrapper;
import com.whiletrue.clustertasks.tasks.recurring.RecurringSchedule;
import com.whiletrue.clustertasks.tasks.recurring.RecurringScheduleStrategy;
import com.whiletrue.clustertasks.tasks.recurring.RecurringScheduleStrategyFixed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;


@DisplayName("In-memory persistence tests")
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class TestInMemoryPersistence extends TestPersistenceBase{

    @BeforeEach
    void init() {
        clusterInstanceNaming = Mockito.mock(ClusterInstanceNaming.class);
        when(clusterInstanceNaming.getInstanceId()).thenReturn("myclusterinstance");
        taskFactory = Mockito.mock(TaskFactory.class);
        fixedTimeProvider.setCurrent(Instant.now());
        taskPersistence = new InMemoryTaskPersistence(clusterInstanceNaming, taskFactory, new ClusterTasksConfigImpl(), fixedTimeProvider);
    }



// TODO: move to base class when implemented in JPA

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

