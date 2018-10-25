package com.whiletrue.sample.controller;

import com.whiletrue.clustertasks.scheduler.ExecutionStats;
import com.whiletrue.clustertasks.tasks.ResourceUsage;
import com.whiletrue.clustertasks.tasks.TaskManager;
import com.whiletrue.sample.BenchmarkConfigurationProperties;
import com.whiletrue.sample.tasks.EmptyTask;
import com.whiletrue.sample.tasks.FailingTask;
import com.whiletrue.sample.tasks.GetUrlTask;
import com.whiletrue.sample.tasks.SingleFullCpuTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

@RestController
@ConditionalOnProperty(value = "benchmark.mode", havingValue = "generator")
@EnableScheduling
public class GeneratorController {
    private static Logger log = LoggerFactory.getLogger(GeneratorController.class);
    private final TaskManager taskManager;
    private final BenchmarkConfigurationProperties benchmarkConfigurationProperties;


    @Autowired
    public GeneratorController(TaskManager taskManager, BenchmarkConfigurationProperties benchmarkConfigurationProperties) {
        this.taskManager = taskManager;
        this.benchmarkConfigurationProperties = benchmarkConfigurationProperties;
    }

    @GetMapping(path = "/admin", produces = "application/json")
    public StatusResponse getStatus() {
        final Map<String, ExecutionStats> performanceSnapshot = taskManager.getPerformanceSnapshot();
        final ResourceUsage resourceUsage = taskManager.getFreeResourcesEstimate();
        return new StatusResponse(performanceSnapshot, resourceUsage);


    }


    @GetMapping(path = "/test-callback/{id}", produces = "application/json")
    public String getCallback(@PathVariable(value = "id") String id) {
        log.info("Received callback from task {}", id);
        return "ok";


    }


    @Scheduled(fixedRate = 1000)
    public void reportCurrentTime() {
        int RESTTaskPerSecond = benchmarkConfigurationProperties.getRESTTaskPerSecond();
        int CPUTasksPerSecond = benchmarkConfigurationProperties.getCPUTasksPerSecond();
        int FailingTasksPerSecond = benchmarkConfigurationProperties.getFailingTasksPerSecond();
        int shortTasksPerSecond = benchmarkConfigurationProperties.getShortTasksPerSecond();


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

        IntStream.range(0, FailingTasksPerSecond).parallel().forEach(i -> {

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

        if (total > 1000) {

            log.warn("Task creation took more than 1 sec");
        }

        final long pendingTasks = taskManager.countPendingTasks();
        log.info("Pending tasks = {}", pendingTasks);

    }
}

