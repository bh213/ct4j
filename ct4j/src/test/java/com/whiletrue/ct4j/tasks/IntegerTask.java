package com.whiletrue.ct4j.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@ClusterTask(name = "integer task", maxRetries = 5, retryDelay = 1000, retryBackoffFactor = 1.5f)
public class IntegerTask extends Task<Integer> {
    private static Logger log = LoggerFactory.getLogger(IntegerTask.class);

    @Override
    public void run(Integer input, TaskExecutionContext taskExecutionContext) throws Exception {
        log.info("Task {} called with input {}", taskExecutionContext.getTaskId(), input);
    }
}
