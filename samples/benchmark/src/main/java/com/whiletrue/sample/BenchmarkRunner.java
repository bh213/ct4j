package com.whiletrue.sample;


import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.whiletrue.clustertasks.tasks.StdTaskRunner;
import com.whiletrue.clustertasks.tasks.TaskManager;
import com.whiletrue.sample.tasks.EmptyTask;
import com.whiletrue.sample.tasks.FailingTask;
import com.whiletrue.sample.tasks.GetUrlTask;
import com.whiletrue.sample.tasks.SingleFullCpuTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;



@Component
public class BenchmarkRunner implements ApplicationRunner {



    private static Logger log = LoggerFactory.getLogger(BenchmarkRunner.class);
    private final JdbcTemplate jdbcTemplate;
    private final TaskManager taskManager;
    private final BenchmarkConfigurationProperties benchmarkConfigurationProperties;



    @Autowired
    public BenchmarkRunner(JdbcTemplate jdbcTemplate, TaskManager taskManager, BenchmarkConfigurationProperties benchmarkConfigurationProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.taskManager = taskManager;
        this.benchmarkConfigurationProperties = benchmarkConfigurationProperties;

    }


    @Override
    public void run(ApplicationArguments args) throws Exception {

        if (benchmarkConfigurationProperties.getMode() == BenchmarkConfigurationProperties.BenchmarkMode.NODE) {
            log.info("Running as node");
            taskManager.startScheduling();
            return;
        }


        if (args.containsOption("nologs")) {
            log.info("Disabling task logs");
            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            loggerContext.getLogger(TestTask.class).setLevel(Level.OFF);
            loggerContext.getLogger(StdTaskRunner.class).setLevel(Level.OFF);
        }

//        if (args.containsOption("cleandb")){
//            log.info("Cleaning db");
//            jdbcTemplate.update("delete from cluster_tasks");
//            jdbcTemplate.update("delete from test_task_table");
//            log.info("done");
//        }


        int RESTTaskPerSecond = benchmarkConfigurationProperties.getRESTTaskPerSecond();
        int CPUTasksPerSecond = benchmarkConfigurationProperties.getCPUTasksPerSecond();
        int FailingTasksPerSecond = benchmarkConfigurationProperties.getFailingTasksPerSecond();
        int shortTasksPerSecond = benchmarkConfigurationProperties.getShortTasksPerSecond();


        if (benchmarkConfigurationProperties.getMode() == BenchmarkConfigurationProperties.BenchmarkMode.GENERATOR) {





            while (true) {
                StopWatch sw = new StopWatch();
                sw.start();

                AtomicInteger totalTasks = new AtomicInteger(0);

                IntStream.range(0, RESTTaskPerSecond).parallel().forEach(i -> {

                    try {
                        taskManager.queueTask(GetUrlTask.class, new GetUrlTask.Input(benchmarkConfigurationProperties.getTestGetUrl()));
                        totalTasks.incrementAndGet();
                    } catch (Exception e) {
                        log.error("Error creating GetUrlTask", e);
                    }

                });

                IntStream.range(0, CPUTasksPerSecond).parallel().forEach(i -> {

                    try {
                        taskManager.queueTask(SingleFullCpuTask.class, new SingleFullCpuTask.Input(500));
                        totalTasks.incrementAndGet();
                    } catch (Exception e) {
                        log.error("Error creating SingleFullCpuTask", e);
                    }

                });

                IntStream.range(0, FailingTasksPerSecond ).parallel().forEach(i -> {

                    try {
                        taskManager.queueTask(FailingTask.class, "failure");
                        totalTasks.incrementAndGet();
                    } catch (Exception e) {
                        log.error("Error creating FailingTask", e);
                    }

                });

                IntStream.range(0, shortTasksPerSecond).parallel().forEach(i -> {

                    try {
                        taskManager.queueTask(EmptyTask.class, "empty");
                        totalTasks.incrementAndGet();
                    } catch (Exception e) {
                        log.error("Error creating EmptyTask", e);
                    }

                });

                sw.stop();
                final long total = sw.getTotalTimeMillis();
                log.info("Task creation: {} tasks took {} ms", totalTasks.get(), total);
                log.info("Settings: REST:{}, CPU:{}, failing:{}, short:{} per second", RESTTaskPerSecond, CPUTasksPerSecond, FailingTasksPerSecond, shortTasksPerSecond);

                if (total < 1000) {

                    Thread.sleep(1000 - total);
                }
                else {
                    log.warn("Task creation took more than 1 sec");
                }
            }
        }


    }


}


