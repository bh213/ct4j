package com.whiletrue.sample;


import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.whiletrue.ct4j.instanceid.ClusterInstance;
import com.whiletrue.ct4j.tasks.BasicTaskInfo;
import com.whiletrue.ct4j.tasks.StdTaskRunner;
import com.whiletrue.ct4j.tasks.TaskCallbacksListener;
import com.whiletrue.ct4j.tasks.TaskManager;
import com.whiletrue.sample.tasks.EmptyTask;
import com.whiletrue.sample.tasks.FailingTask;
import com.whiletrue.sample.tasks.GetUrlTask;
import com.whiletrue.sample.tasks.SingleFullCpuTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;


@Component
public class BenchmarkRunner implements ApplicationRunner {


    private static Logger log = LoggerFactory.getLogger(BenchmarkRunner.class);
    private final TaskManager taskManager;
    private final BenchmarkConfigurationProperties benchmarkConfigurationProperties;


    @Autowired
    public BenchmarkRunner(TaskManager taskManager, BenchmarkConfigurationProperties benchmarkConfigurationProperties) {
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

            @Override
            public void clusterNodeStarted(ClusterInstance clusterInstance) {
                log.info("Callback: node started {}", clusterInstance);
            }

            @Override
            public void clusterNodeStopped(ClusterInstance clusterInstance) {
                log.info("Callback: node stopped{}", clusterInstance);

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

            log.info("Running as generator, task scheduling disabled by default");
            //taskManager.stopScheduling();

        }


    }


}


