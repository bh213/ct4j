package com.whiletrue.sample;


import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.whiletrue.clustertasks.tasks.BasicTaskInfo;
import com.whiletrue.clustertasks.tasks.StdTaskRunner;
import com.whiletrue.clustertasks.tasks.TaskCallbacksListener;
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


        taskManager.setCallbacksListener(new TaskCallbacksListener() {
            @Override
            public void taskOverdue(BasicTaskInfo task) {
                log.error("Callback: task overdue id {}, running time {} ms, max running time {} ms", task.getTaskId(), task.getRunningTimeInMs(), task.getMaxRunningTimeInMilliseconds());
            }

            @Override
            public void taskCompleted(BasicTaskInfo task) {
                log.info("Callback: task completed {}", task.getTaskId());
            }

            @Override
            public void taskFailed(BasicTaskInfo task) {
                log.error("Callback: task failed {}", task.getTaskId());
            }
        });

        if (benchmarkConfigurationProperties.getMode() == BenchmarkConfigurationProperties.BenchmarkMode.NODE) {
            log.info("Running as node, task scheduling enabled");
            taskManager.startScheduling();
            return;
        }


        if (benchmarkConfigurationProperties.isDisableTaskLogs()) {
            log.info("Disabling task logs");
            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            loggerContext.getLogger(EmptyTask.class).setLevel(Level.OFF);
            loggerContext.getLogger(FailingTask.class).setLevel(Level.OFF);
            loggerContext.getLogger(GetUrlTask.class).setLevel(Level.OFF);
            loggerContext.getLogger(SingleFullCpuTask.class).setLevel(Level.OFF);
            loggerContext.getLogger(StdTaskRunner.class).setLevel(Level.OFF);
        }

        if (benchmarkConfigurationProperties.getMode() == BenchmarkConfigurationProperties.BenchmarkMode.GENERATOR) {

            log.info("Running as generator, task scheduling disabled");
            taskManager.stopScheduling();

        }


    }


}


