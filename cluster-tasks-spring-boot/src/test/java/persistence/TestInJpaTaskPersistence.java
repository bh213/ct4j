package persistence;


import com.whiletrue.clustertasks.spring.*;
import com.whiletrue.clustertasks.instanceid.ClusterInstance;
import com.whiletrue.clustertasks.spring.JPA.ClusterInstanceRepository;
import com.whiletrue.clustertasks.spring.JPA.ClusterTaskEntity;
import com.whiletrue.clustertasks.spring.JPA.ClusterTaskRepository;
import com.whiletrue.clustertasks.spring.JPA.JpaClusterTaskPersistence;
import com.whiletrue.clustertasks.tasks.ClusterTasksConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;

import static org.mockito.Mockito.when;


@DisplayName("spring tasks persistence tests")
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@ExtendWith(SpringExtension.class)
@DataJpaTest()
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@SpringBootTest(classes = TestInJpaTaskPersistence.TestApp.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class TestInJpaTaskPersistence extends TestPersistenceBase {
    @Autowired
    private ClusterInstanceRepository clusterInstanceRepository;
    @Autowired
    private ClusterTaskRepository clusterTaskRepository;
    @Autowired
    private AutowireCapableBeanFactory applicationContext;
    public TestInJpaTaskPersistence() {
    }

    @BeforeEach
    void init() {
        clusterInstance = Mockito.mock(ClusterInstance.class);
        when(clusterInstance.getInstanceId()).thenReturn("myclusterinstance");
        taskFactory = new SpringTaskFactory(applicationContext);
        fixedTimeProvider.setCurrent(Instant.now());
        taskPersistence = new JpaClusterTaskPersistence(clusterTaskRepository, clusterInstanceRepository, clusterInstance, taskFactory, new ClusterTasksConfig(), fixedTimeProvider);
    }

    @SpringBootApplication
    @EnableJpaRepositories(basePackageClasses = ClusterInstanceRepository.class)
    @EntityScan(basePackageClasses = ClusterTaskEntity.class)
    public static class TestApp {
        public static void main(String[] args) {
            SpringApplication.run(TestApp.class, args);
        }
    }
}

