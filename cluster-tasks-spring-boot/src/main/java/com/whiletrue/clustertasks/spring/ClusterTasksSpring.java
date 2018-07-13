package com.whiletrue.clustertasks.spring;

import com.whiletrue.clustertasks.factory.TaskFactory;
import com.whiletrue.clustertasks.inmemory.DefaultConstructorTaskFactory;
import com.whiletrue.clustertasks.inmemory.InMemoryTaskPersistence;
import com.whiletrue.clustertasks.instanceid.ClusterInstance;
import com.whiletrue.clustertasks.spring.JPA.ClusterInstanceRepository;
import com.whiletrue.clustertasks.spring.JPA.ClusterTaskEntity;
import com.whiletrue.clustertasks.spring.JPA.ClusterTaskRepository;
import com.whiletrue.clustertasks.spring.JPA.JpaClusterTaskPersistence;
import com.whiletrue.clustertasks.tasks.*;
import com.whiletrue.clustertasks.instanceid.NetworkClusterInstance;
import com.whiletrue.clustertasks.scheduler.Scheduler;
import com.whiletrue.clustertasks.scheduler.InternalTaskEvents;
import com.whiletrue.clustertasks.timeprovider.LocalTimeProvider;
import com.whiletrue.clustertasks.timeprovider.TimeProvider;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import javax.annotation.PostConstruct;

@Configuration
@EnableConfigurationProperties(ClusterTasksConfigurationProperties.class)
@EnableJpaRepositories(basePackageClasses = ClusterTaskRepository.class)
@EntityScan(basePackageClasses= ClusterTaskEntity.class)
@Import(SpringTaskFactory.class)
public class ClusterTasksSpring implements BeanFactoryAware {

    private final ClusterTasksConfigurationProperties configurationProperties;
    private TaskPersistence clusterTaskPersistence;

    private TaskFactory taskFactory;
    private ClusterTasksConfig clusterTasksConfig;
    private TimeProvider timeProvider;
    private BeanFactory beanFactory;


    public ClusterTasksSpring( ClusterTasksConfigurationProperties configurationProperties) {
        this.configurationProperties = configurationProperties;
    }

    @PostConstruct
    public void init() {

        switch(configurationProperties.getTimeProvider()) {
            case "local" : this.timeProvider = new LocalTimeProvider(); break;
            default: {
                try {
                    Class clazz = Class.forName(configurationProperties.getTaskFactory());
                    this.timeProvider  = (TimeProvider) this.beanFactory.getBean(clazz);
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException("ct4j: timeProvider class could not be found", e);
                }
            }
        }

        switch(configurationProperties.getTaskFactory()) {
            case "constructor" : this.taskFactory = new DefaultConstructorTaskFactory(); break;
            case "spring" : this.taskFactory = this.beanFactory.getBean(SpringTaskFactory.class); break;
            default: {
                try {
                    Class clazz = Class.forName(configurationProperties.getTaskFactory());
                    this.taskFactory  = (TaskFactory) this.beanFactory.getBean(clazz);
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException("ct4j: taskFactory class could not be found", e);
                }
            }
        }

        clusterTasksConfig = new ClusterTasksConfig();
        ClusterInstance clusterInstance = new NetworkClusterInstance();

        switch(configurationProperties.getPersistence()) {
            case "memory" : this.clusterTaskPersistence = new InMemoryTaskPersistence(clusterInstance, taskFactory, clusterTasksConfig, timeProvider); break;
            case "jpa" : {
                ClusterTaskRepository clusterTaskRepository = this.beanFactory.getBean(ClusterTaskRepository.class);
                ClusterInstanceRepository clusterInstanceRepository = this.beanFactory.getBean(ClusterInstanceRepository.class);;
                this.clusterTaskPersistence = new JpaClusterTaskPersistence(clusterTaskRepository, clusterInstanceRepository, clusterInstance, taskFactory, clusterTasksConfig, timeProvider);
                break;
            }
            default: {
                try {
                    Class clazz = Class.forName(configurationProperties.getPersistence());
                    this.clusterTaskPersistence  = (TaskPersistence) this.beanFactory.getBean(clazz);
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException("ct4j: persistence class could not be found", e);
                }
            }
        }
    }


    @Bean
    public TaskManager getTaskManager(){

        TaskRunner taskRunner = new StdTaskRunner(clusterTaskPersistence, clusterTasksConfig, timeProvider);
        Scheduler scheduler = new Scheduler(clusterTaskPersistence, taskRunner, clusterTasksConfig);
        TaskManager taskManager = new StdTaskManager(clusterTaskPersistence, taskFactory, scheduler);
        return taskManager;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }
}
