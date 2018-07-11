package persistence;


import com.whiletrue.clustertasks.JPA.*;
import com.whiletrue.clustertasks.instanceid.ClusterInstance;
import com.whiletrue.clustertasks.tasks.ClusterTasksConfig;
import config.FixedTimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;

import static org.mockito.Mockito.when;


@DisplayName("JPA tasks persistence tests")
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
    private ApplicationContext applicationContext;
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

