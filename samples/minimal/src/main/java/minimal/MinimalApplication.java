package minimal;

import com.whiletrue.clustertasks.spring.EnableCt4j;
import com.whiletrue.clustertasks.tasks.TaskManager;
import com.whiletrue.clustertasks.tasks.TaskStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;

@EnableCt4j
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
            var list = new ArrayList<String>();
            list.add(taskManager.queueTask(SampleTask.class, "one"));
            list.add(taskManager.queueTask(SampleTask.class, "two"));
            list.add(taskManager.queueTask(SampleTask.class, "three"));


            while (!list.stream().allMatch(taskId-> taskManager.getTaskStatus(taskId) == TaskStatus.Success)) {
                Thread.sleep(100);
            }

            taskManager.stopScheduling();

            System.exit(0);

        };
    }

}