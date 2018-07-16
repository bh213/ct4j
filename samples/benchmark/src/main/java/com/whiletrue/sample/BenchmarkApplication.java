package com.whiletrue.sample;

import com.whiletrue.clustertasks.spring.EnableClusterTasks;
import com.whiletrue.clustertasks.tasks.TaskManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableClusterTasks
@SpringBootApplication
@EntityScan
@EnableJpaRepositories
public class BenchmarkApplication {

    private TaskManager taskManager;
    @Autowired
    public BenchmarkApplication(TaskManager taskManager) {
        this.taskManager = taskManager;
    }
    public static void main(String[] args) {
        SpringApplication.run(BenchmarkApplication.class, args);
    }

}
