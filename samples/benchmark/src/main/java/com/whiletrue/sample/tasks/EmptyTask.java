package com.whiletrue.sample.tasks;

import com.whiletrue.ct4j.tasks.Task;
import com.whiletrue.ct4j.tasks.TaskConfig;
import com.whiletrue.ct4j.tasks.TaskExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *  Very short task that test ct4j infrastructure
 */
public class EmptyTask extends Task<String> {
    private static Logger log = LoggerFactory.getLogger(EmptyTask.class);


    @Override
    public TaskConfig configureTask(TaskConfig.TaskConfigBuilder builder) {
        return builder
                .setPriority(10000)
                .setRetryDelay(100, 5)
                .setMaxRetries(3)
                .estimateResourceUsage(0.1f, 1)
                .build();
    }

    @Override
    public void run(String input, TaskExecutionContext taskExecutionContext) throws Exception {
        log.info("Short task {} is done", taskExecutionContext.getTaskId());
    }



}
