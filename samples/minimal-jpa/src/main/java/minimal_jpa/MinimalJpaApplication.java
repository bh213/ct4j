package minimal_jpa;

import com.whiletrue.ct4j.spring.EnableCt4j;
import com.whiletrue.ct4j.tasks.ScheduledTaskAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import com.whiletrue.ct4j.tasks.TaskManager;

@EnableCt4j
@SpringBootApplication
public class MinimalJpaApplication {


    private TaskManager taskManager;

    @Autowired
    public MinimalJpaApplication(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    public static void main(String[] args) {
        SpringApplication.run(MinimalJpaApplication.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
        return args -> {

        taskManager.startScheduling();
        taskManager.queueTask(SampleTask.class, "one");
        taskManager.queueTask(SampleTask.class, "two");
        taskManager.queueTask(SampleTask.class, "three");
        taskManager.registerScheduledTask(SampleTask.class, "recurring", 3000, ScheduledTaskAction.SingletonTaskKeepExisting);

        };
    }

}