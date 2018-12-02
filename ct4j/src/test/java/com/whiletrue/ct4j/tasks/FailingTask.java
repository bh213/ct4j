package com.whiletrue.ct4j.tasks;

public class FailingTask extends Task<String> {

    @Override
    public void run(String s, TaskExecutionContext taskExecutionContext) throws Exception {
        throw new Exception("This task always fails");
    }
}
