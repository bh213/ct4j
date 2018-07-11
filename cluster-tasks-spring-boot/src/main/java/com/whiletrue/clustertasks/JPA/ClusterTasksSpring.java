package com.whiletrue.clustertasks.JPA;

import com.whiletrue.clustertasks.factory.TaskFactory;
import com.whiletrue.clustertasks.instanceid.ClusterInstance;
import com.whiletrue.clustertasks.tasks.*;
import com.whiletrue.clustertasks.instanceid.NetworkClusterInstance;
import com.whiletrue.clustertasks.scheduler.Scheduler;
import com.whiletrue.clustertasks.scheduler.InternalTaskEvents;
import com.whiletrue.clustertasks.timeprovider.LocalTimeProvider;
import com.whiletrue.clustertasks.timeprovider.TimeProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackageClasses = ClusterTaskRepository.class)
@EntityScan(basePackageClasses=ClusterTaskEntity.class)
@Import(SpringTaskFactory.class)
public class ClusterTasksSpring  {

    private JpaClusterTaskPersistence clusterTaskPersistence;
    private ClusterInstance clusterInstance;
    private ClusterTaskRepository clusterTaskRepository;
    private ClusterInstanceRepository clusterInstanceRepository;
    private TaskRunner taskRunner;
    private Scheduler scheduler;

    private TaskFactory taskFactory;
    private TaskManager taskManager;
    private InternalTaskEvents internalTaskEvents;
    private ClusterTasksConfig clusterTasksConfig;
    private TimeProvider timeProvider;

    @Autowired
    public ClusterTasksSpring(ClusterTaskRepository clusterTaskRepository, ClusterInstanceRepository clusterInstanceRepository, TaskFactory taskFactory) {
        this.clusterTaskRepository = clusterTaskRepository;
        this.clusterInstanceRepository = clusterInstanceRepository;
        this.taskFactory = taskFactory;
    }

    @Bean
    public TaskManager getTaskManager(){
        timeProvider = new LocalTimeProvider();
        clusterTasksConfig = new ClusterTasksConfig();
        clusterInstance = new NetworkClusterInstance();
        clusterTaskPersistence = new JpaClusterTaskPersistence(clusterTaskRepository, clusterInstanceRepository, clusterInstance, taskFactory, clusterTasksConfig, timeProvider);
        taskRunner = new StdTaskRunner(clusterTaskPersistence, clusterTasksConfig, timeProvider);
        scheduler = new Scheduler(clusterTaskPersistence, taskRunner, clusterTasksConfig);
        taskManager = new StdTaskManager(clusterTaskPersistence,  taskFactory, scheduler);
        return taskManager;
    }
}
