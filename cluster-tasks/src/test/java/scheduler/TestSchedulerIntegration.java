package scheduler;


import com.whiletrue.clustertasks.factory.TaskFactory;
import com.whiletrue.clustertasks.inmemory.DefaultConstructorTaskFactory;
import com.whiletrue.clustertasks.inmemory.InMemoryTaskPersistence;
import com.whiletrue.clustertasks.instanceid.ClusterInstance;
import com.whiletrue.clustertasks.scheduler.Scheduler;
import com.whiletrue.clustertasks.tasks.*;
import config.FixedTimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import tasks.NoOpTestTask;

import java.time.Instant;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
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
        final ClusterTasksConfig clusterTasksConfig = new ClusterTasksConfig();
        taskPersistence = new InMemoryTaskPersistence(clusterInstance, taskFactory, clusterTasksConfig, fixedTimeProvider);
        taskRunner = new StdTaskRunner(taskPersistence, clusterTasksConfig, fixedTimeProvider);
        scheduler = new Scheduler(taskPersistence, taskRunner, clusterTasksConfig);
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
    @DisplayName("test multiple tasks")
    public void testMultipleTasksTask() throws Exception {
        ArrayList<String> tasks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final String taskId = taskPersistence.queueTask(new NoOpTestTask(), "very valid string");
            tasks.add(taskId);
        }

        scheduler.startScheduling();
        Thread.sleep(100);
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
