package com.whiletrue.clustertasks.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@ClusterTask(name = "example task", maxRetries = 5, retryDelay = 1000, retryBackoffFactor = 1.5f)
public class ExampleTask extends Task<String> {
    private static Logger log = LoggerFactory.getLogger(ExampleTask.class);

    @Override
    public void run(String input, TaskExecutionContext taskExecutionContext) throws Exception {
        log.info("Task {} called with input {}", taskExecutionContext.getTaskId(), input);
    }
}
