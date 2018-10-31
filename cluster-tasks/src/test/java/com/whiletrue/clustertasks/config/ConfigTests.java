package com.whiletrue.clustertasks.config;

import com.whiletrue.clustertasks.factory.TaskFactory;
import com.whiletrue.clustertasks.inmemory.InMemoryTaskPersistence;
import com.whiletrue.clustertasks.instanceid.ClusterInstanceNaming;
import com.whiletrue.clustertasks.tasks.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import com.whiletrue.clustertasks.tasks.NoOpTestTask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DisplayName("Task configuration tests")
public class ConfigTests {

    private ClusterTasksConfig clusterTasksConfig;
    private InMemoryTaskPersistence taskPersistence;
    private ClusterInstanceNaming clusterInstanceNaming;
    private TaskFactory taskFactory;


    @BeforeEach
    void init() {
        clusterInstanceNaming = Mockito.mock(ClusterInstanceNaming.class);
        when(clusterInstanceNaming.getInstanceId()).thenReturn("myclusterinstance");
        taskFactory = Mockito.mock(TaskFactory.class);

        clusterTasksConfig = new ClusterTasksConfigImpl();
        taskPersistence = new InMemoryTaskPersistence(clusterInstanceNaming, taskFactory, clusterTasksConfig, new FixedTimeProvider());
    }


    @Test
    @DisplayName("Test defaults")
    public void testDefaultConfigs() throws Exception {
        final String id = taskPersistence.queueTask(new NoOpTestTask(), null);
        final TaskWrapper<?> task = taskPersistence.getTask(id);
        assertThat(task).isNotNull();
        assertThat(task.getTaskExecutionContext()).isNotNull();
        assertThat(task.getTaskExecutionContext().getTaskName()).isEqualTo(NoOpTestTask.class.getName());
        assertThat(task.getTaskConfig()).isNotNull();
        assertThat(task.getTaskConfig().getPriority()).isEqualTo(clusterTasksConfig.getDefaultPriority());
        assertThat(task.getTaskConfig().getRetryPolicy()).isNotNull();
        assertThat(task.getTaskConfig().getRetryPolicy().getMaxRetries()).isEqualTo(clusterTasksConfig.getDefaultRetries());
        assertThat(task.getTaskConfig().getRetryPolicy().getRetryDelay()).isEqualTo(clusterTasksConfig.getDefaultRetryDelay());
        assertThat(task.getTaskConfig().getRetryPolicy().getRetryBackoffFactor()).isEqualTo(clusterTasksConfig.getDefaultRetryBackoffFactor());
    }


    @ClusterTask(name = "AnnotatedConfigTestTask-1", defaultPriority = 3234, maxRetries = 33,retryBackoffFactor = 2.13f,retryDelay = 3000)
    public class AnnotatedConfigTestTask extends Task {

        @Override
        public void run(Object o, TaskExecutionContext taskExecutionContext) throws Exception {
        }
    }

    @Test
    @DisplayName("Test configuration through annotations")
    public void testAnnotationConfigs() throws Exception {
        final String id = taskPersistence.queueTask(new AnnotatedConfigTestTask(), null);
        final TaskWrapper<?> task = taskPersistence.getTask(id);
        assertThat(task).isNotNull();
        assertThat(task.getTaskExecutionContext()).isNotNull();
        assertThat(task.getTaskExecutionContext().getTaskName()).isEqualTo("AnnotatedConfigTestTask-1");
        assertThat(task.getTaskConfig()).isNotNull();
        assertThat(task.getTaskConfig().getPriority()).isEqualTo(3234);
        assertThat(task.getTaskConfig().getRetryPolicy()).isNotNull();
        assertThat(task.getTaskConfig().getRetryPolicy().getMaxRetries()).isEqualTo(33);
        assertThat(task.getTaskConfig().getRetryPolicy().getRetryDelay()).isEqualTo(3000);
        assertThat(task.getTaskConfig().getRetryPolicy().getRetryBackoffFactor()).isEqualTo(2.13f);
    }


    public class ConfiguredConfigTestTask extends Task {

        @Override
        public TaskConfig configureTask(TaskConfig.TaskConfigBuilder builder) {
            builder.setTaskName("configured1")
                    .setMaxRetries(22)
                    .setPriority(42000)
                    .setRetryDelay(4000, 4);
            return builder.build();
        }

        @Override
        public void run(Object o, TaskExecutionContext taskExecutionContext) throws Exception {
        }
    }

    @Test
    @DisplayName("Test configuration through configureTask")
    public void testConfiguredTask() throws Exception {
        testConfiguredTask(new ConfiguredConfigTestTask());
    }

    @ClusterTask(name = "AnnotatedConfigTestTask-1", defaultPriority = 3234, maxRetries = 33,retryBackoffFactor = 2.13f,retryDelay = 3000)
    public class ConfiguredAnnotatedConfigTestTask extends Task {

        @Override
        public TaskConfig configureTask(TaskConfig.TaskConfigBuilder builder) {
            builder.setTaskName("configured1")
                    .setMaxRetries(22)
                    .setPriority(42000)
                    .setRetryDelay(4000, 4);
            return builder.build();
        }

        @Override
        public void run(Object o, TaskExecutionContext taskExecutionContext) throws Exception {
        }
    }


    @Test
    @DisplayName("Test task with configureTask and annotation")
    public void testConfiguredAnnotatedTask() throws Exception {
        testConfiguredTask(new ConfiguredAnnotatedConfigTestTask());
    }

    private void testConfiguredTask(Task taskInstance) throws Exception {
        final String id = taskPersistence.queueTask(taskInstance, null);
        final TaskWrapper<?> task = taskPersistence.getTask(id);
        assertThat(task).isNotNull();
        assertThat(task.getTaskExecutionContext()).isNotNull();
        assertThat(task.getTaskExecutionContext().getTaskName()).isEqualTo("configured1");
        assertThat(task.getTaskConfig()).isNotNull();
        assertThat(task.getTaskConfig().getPriority()).isEqualTo(42000);
        assertThat(task.getTaskConfig().getRetryPolicy()).isNotNull();
        assertThat(task.getTaskConfig().getRetryPolicy().getMaxRetries()).isEqualTo(22);
        assertThat(task.getTaskConfig().getRetryPolicy().getRetryDelay()).isEqualTo(4000);
        assertThat(task.getTaskConfig().getRetryPolicy().getRetryBackoffFactor()).isEqualTo(4);
    }


}
