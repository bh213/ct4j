package minimal_jpa_clustered;

import com.whiletrue.clustertasks.spring.EnableClusterTasks;
import com.whiletrue.clustertasks.tasks.TaskManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@EnableClusterTasks
@SpringBootApplication
public class MinimalJpaClusteredApplication {

    private TaskManager taskManager;

    @Autowired
    public MinimalJpaClusteredApplication(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    public static void main(String[] args) {
        SpringApplication.run(MinimalJpaClusteredApplication.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
        return args -> {
        taskManager.startScheduling();
        taskManager.queueTask(SampleTask.class, "one");
        taskManager.queueTask(SampleTask.class, "two");
        taskManager.queueTask(SampleTask.class, "three");
        };
    }

}