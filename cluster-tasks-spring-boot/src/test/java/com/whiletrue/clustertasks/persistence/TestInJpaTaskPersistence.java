package com.whiletrue.clustertasks.persistence;


import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.whiletrue.clustertasks.instanceid.ClusterInstanceNaming;
import com.whiletrue.clustertasks.spring.JPA.ClusterInstanceRepository;
import com.whiletrue.clustertasks.spring.JPA.ClusterTaskEntity;
import com.whiletrue.clustertasks.spring.JPA.ClusterTaskRepository;
import com.whiletrue.clustertasks.spring.JPA.JpaClusterTaskPersistence;
import com.whiletrue.clustertasks.spring.SpringTaskFactory;
import com.whiletrue.clustertasks.tasks.ClusterTasksConfigImpl;
import com.whiletrue.clustertasks.tasks.WrongInputClassTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;


@DisplayName("spring tasks persistence tests")
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@SpringBootTest(classes = TestInJpaTaskPersistence.TestApp.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class TestInJpaTaskPersistence extends TestPersistenceBase {
    @Autowired
    private ClusterTaskRepository clusterTaskRepository;
    @Autowired
    private AutowireCapableBeanFactory applicationContext;
    public TestInJpaTaskPersistence() {
    }

    @BeforeEach
    void init() {
        clusterInstanceNaming = Mockito.mock(ClusterInstanceNaming.class);
        when(clusterInstanceNaming.getInstanceId()).thenReturn("myclusterinstance");
        taskFactory = new SpringTaskFactory(applicationContext);
        fixedTimeProvider.setCurrent(Instant.now());
        taskPersistence = new JpaClusterTaskPersistence(clusterTaskRepository, null, clusterInstanceNaming, taskFactory, new ClusterTasksConfigImpl(), fixedTimeProvider);
    }

    @SpringBootApplication
    @EnableJpaRepositories(basePackageClasses = ClusterInstanceRepository.class)
    @EntityScan(basePackageClasses = ClusterTaskEntity.class)
    public static class TestApp {
        public static void main(String[] args) {
            SpringApplication.run(TestApp.class, args);
        }
    }


    @Test
    @DisplayName("Test task validation")
    public void createTaskValidInput() throws Exception {

        assertThatThrownBy( ()-> taskPersistence.queueTask(new WrongInputClassTask(),  new WrongInputClassTask.InputWithoutDefaultConstructor("very valid string")))
                .hasCauseInstanceOf(MismatchedInputException.class)
                .hasMessageContaining("Did you forget to add default constructor to task input class?");

    }


}

