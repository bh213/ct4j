package TaskManager;

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
import tasks.ExampleTask;
import tasks.IntegerTask;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DisplayName("Scheduler tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestTaskManager {

    protected TaskPersistence taskPersistence;
    protected ClusterInstance clusterInstance;
    protected TaskFactory taskFactory;
    protected FixedTimeProvider fixedTimeProvider = new FixedTimeProvider();
    protected Scheduler scheduler;
    protected TaskRunner taskRunner;
    protected TaskManager taskManager;


    @BeforeEach
    void init() {
        clusterInstance = Mockito.mock(ClusterInstance.class);

        when(clusterInstance.getInstanceId()).thenReturn("myclusterinstance");

        taskFactory = new DefaultConstructorTaskFactory();
        fixedTimeProvider.setCurrent(Instant.now());
        final ClusterTasksConfig clusterTasksConfig = new ClusterTasksConfig();
        taskPersistence = new InMemoryTaskPersistence(clusterInstance, taskFactory, clusterTasksConfig, fixedTimeProvider);
        taskRunner = new StdTaskRunner(taskPersistence, clusterTasksConfig, fixedTimeProvider);
        scheduler = new Scheduler(taskPersistence,taskRunner, clusterTasksConfig);
        taskManager = new StdTaskManager(taskPersistence, taskFactory, scheduler);
    }

    @Test
    @DisplayName("test example task")
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
    @DisplayName("test integer task")
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




}
