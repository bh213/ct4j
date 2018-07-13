package minimal;

import java.util.Arrays;

import com.whiletrue.clustertasks.spring.EnableClusterTasks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import com.whiletrue.clustertasks.tasks.TaskManager;

@EnableClusterTasks
@SpringBootApplication
public class MinimalApplication {


    private TaskManager taskManager;

    @Autowired
    public MinimalApplication(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    public static void main(String[] args) {
        SpringApplication.run(MinimalApplication.class, args);
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