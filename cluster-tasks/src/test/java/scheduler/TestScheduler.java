package scheduler;


import com.whiletrue.clustertasks.factory.TaskFactory;
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

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


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
        final ClusterTasksConfig clusterTasksConfig = new ClusterTasksConfig();
        taskPersistence = new InMemoryTaskPersistence(clusterInstance, taskFactory, clusterTasksConfig, fixedTimeProvider);
        taskRunner = Mockito.mock(TaskRunner.class);
        scheduler = new Scheduler(taskPersistence,taskRunner, clusterTasksConfig);
    }



    @Test
    @DisplayName("test resource estimate")
    public void testFreeTasksSlots() {
        final ResourceUsage resourceUsage = new ResourceUsage(77, 78);
        when(taskRunner.getCurrentResourcesAvailable()).thenReturn(resourceUsage);
        assertThat(scheduler.getFreeResourcesEstimate()).isEqualTo(resourceUsage);
        assertThat(scheduler.getFreeResourcesEstimate().getCpuCoreUsage()).isEqualTo(77);
        assertThat(scheduler.getFreeResourcesEstimate().getMaximumMemoryUsageInMb()).isEqualTo(78);
    }







}
