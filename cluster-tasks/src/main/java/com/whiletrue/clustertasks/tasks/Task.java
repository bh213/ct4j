package com.whiletrue.clustertasks.tasks;

public abstract class Task<INPUT> {
    public TaskConfig configureTask(TaskConfig.TaskConfigBuilder builder){return builder.build();};
    public abstract void run(INPUT input, TaskExecutionContext taskExecutionContext) throws Exception;

    public RetryPolicy onError(INPUT input, Exception exception, RetryPolicy currentRetryPolicy) { return null;}
    public void onSuccess(INPUT input){}
    public void onFailure(INPUT input) {}
}
