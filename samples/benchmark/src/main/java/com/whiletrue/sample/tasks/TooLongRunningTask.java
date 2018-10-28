package com.whiletrue.sample.tasks;

import com.whiletrue.clustertasks.tasks.Task;
import com.whiletrue.clustertasks.tasks.TaskConfig;
import com.whiletrue.clustertasks.tasks.TaskExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *  Very short task that test ct4j infrastructure
 */
public class TooLongRunningTask extends Task<String> {
    private static Logger log = LoggerFactory.getLogger(TooLongRunningTask.class);


    @Override
    public TaskConfig configureTask(TaskConfig.TaskConfigBuilder builder) {
        return builder
                .setPriority(10000)
                .setRetryDelay(100, 5)
                .setMaxRetries(3)
                .setMaxRunningtime(3000)
                .estimateResourceUsage(0.1f, 1)
                .build();
    }

    @Override
    public void run(String input, TaskExecutionContext taskExecutionContext) throws Exception {
        log.info("Too long running task {} will run for 15 sec", taskExecutionContext.getTaskId());
        Thread.sleep(15*1000);
        log.info("Too long running task {} done", taskExecutionContext.getTaskId());
    }



}
