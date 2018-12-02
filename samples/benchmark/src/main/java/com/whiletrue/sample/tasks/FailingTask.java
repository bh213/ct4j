package com.whiletrue.sample.tasks;

import com.whiletrue.ct4j.tasks.Task;
import com.whiletrue.ct4j.tasks.TaskConfig;
import com.whiletrue.ct4j.tasks.TaskExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *  Task that always fails
 */
public class FailingTask extends Task<String> {
    private static Logger log = LoggerFactory.getLogger(FailingTask.class);


    @Override
    public TaskConfig configureTask(TaskConfig.TaskConfigBuilder builder) {
        return builder
                .setPriority(500)
                .setRetryDelay(1000, 2)
                .setMaxRetries(3)
                .estimateResourceUsage(0.1f, 1)
                .build();
    }

    @Override
    public void run(String input, TaskExecutionContext taskExecutionContext) throws Exception {

        log.info("This task will fail, retry {}", taskExecutionContext.getRetry());

        throw new Exception("This is failing task error");


    }



}
