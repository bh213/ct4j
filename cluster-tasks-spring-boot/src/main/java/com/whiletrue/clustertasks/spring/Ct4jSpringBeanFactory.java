package com.whiletrue.clustertasks.spring;

import com.whiletrue.clustertasks.factory.TaskFactory;
import com.whiletrue.clustertasks.inmemory.DefaultConstructorTaskFactory;
import com.whiletrue.clustertasks.inmemory.InMemoryTaskPersistence;
import com.whiletrue.clustertasks.instanceid.ClusterInstanceNaming;
import com.whiletrue.clustertasks.spring.JPA.*;
import com.whiletrue.clustertasks.tasks.*;
import com.whiletrue.clustertasks.instanceid.NetworkClusterInstanceNaming;
import com.whiletrue.clustertasks.scheduler.Scheduler;
import com.whiletrue.clustertasks.timeprovider.LocalTimeProvider;
import com.whiletrue.clustertasks.timeprovider.TimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
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
@EntityScan(basePackageClasses = ClusterTaskEntity.class)
@Import(SpringTaskFactory.class)
public class Ct4jSpringBeanFactory implements BeanFactoryAware {

    private static Logger log = LoggerFactory.getLogger(Ct4jSpringBeanFactory.class);
    private final ClusterTasksConfigurationProperties configurationProperties;
    private TaskPersistence clusterTaskPersistence;

    private TaskFactory taskFactory;
    private ClusterTasksConfig clusterTasksConfig;
    private TimeProvider timeProvider;
    private BeanFactory beanFactory;


    @Autowired
    public Ct4jSpringBeanFactory(ClusterTasksConfigurationProperties configurationProperties) {
        this.configurationProperties = configurationProperties;
    }

    @PostConstruct
    public void init() {

        switch(configurationProperties.getTimeProvider()) {
            case "local" : this.timeProvider = new LocalTimeProvider(); break;
            default: {
                try {
                    Class<?> clazz = Class.forName(configurationProperties.getTaskFactory());
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
                    Class<?> clazz = Class.forName(configurationProperties.getTaskFactory());
                    this.taskFactory  = (TaskFactory) this.beanFactory.getBean(clazz);
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException("ct4j: taskFactory class could not be found", e);
                }
            }
        }

        clusterTasksConfig = new ClusterTasksConfigImpl();
        ClusterInstanceNaming clusterInstanceNaming = new NetworkClusterInstanceNaming();

        switch(configurationProperties.getPersistence()) {
            case "memory" : this.clusterTaskPersistence = new InMemoryTaskPersistence(clusterInstanceNaming, taskFactory, clusterTasksConfig, timeProvider); break;
            case "jpa" : {
                ClusterTaskRepository clusterTaskRepository = this.beanFactory.getBean(ClusterTaskRepository.class);
                ClusterInstanceRepository clusterInstanceRepository = this.beanFactory.getBean(ClusterInstanceRepository.class);
                final JpaClusterNodePersistence jpaClusterNodePersistence = new JpaClusterNodePersistence(clusterInstanceRepository, clusterInstanceNaming, clusterTasksConfig, timeProvider);
                this.clusterTaskPersistence = new JpaClusterTaskPersistence(clusterTaskRepository, jpaClusterNodePersistence, clusterInstanceNaming, taskFactory, clusterTasksConfig, timeProvider);

                break;
            }
            default: {
                try {
                    Class<?> clazz = Class.forName(configurationProperties.getPersistence());
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
        Scheduler scheduler = new Scheduler(clusterTaskPersistence, taskRunner, clusterTasksConfig, timeProvider);
        TaskManager taskManager = new StdTaskManager(clusterTaskPersistence, taskFactory, scheduler);

        log.info("\n"+
                "================================================================================\n" +
                "  ct4j (Cluster tasks for Java)                                                 \n" +
                "\n"+
                "  Time provider:      " + timeProvider.getClass().getSimpleName()+"\n" +
                "  Task factory:       " + taskFactory.getClass().getSimpleName()+"\n" +
                "  Persistence:        " + clusterTaskPersistence.getClass().getSimpleName()+"\n" +
                "      Persistent:     " + clusterTaskPersistence.isPersistent()+"\n" +
                "      Clustered:      " + clusterTaskPersistence.isClustered()+"\n" +
                "  Persistence:        " + clusterTaskPersistence.getClass().getSimpleName()+"\n" +
                "  Task runner:        " + taskRunner.getClass().getSimpleName()+"\n" +
                "  Scheduler:          " + scheduler.getClass().getSimpleName()+"\n" +
                "  Avail. resources:   " + taskRunner.getCurrentResourcesAvailable().toString()+"\n" +
                "================================================================================\n");
        return taskManager;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }
}
