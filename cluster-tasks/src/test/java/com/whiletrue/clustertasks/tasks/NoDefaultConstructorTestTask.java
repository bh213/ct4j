package com.whiletrue.clustertasks.tasks;

public class NoDefaultConstructorTestTask extends Task {

    private final String argument;

    public String getArgument() {
        return argument;
    }

    public NoDefaultConstructorTestTask(String argument) {
        this.argument = argument;
    }

    @Override
    public TaskConfig configureTask(TaskConfig.TaskConfigBuilder builder) {
        return null;
    }

    @Override
    public void run(Object o, TaskExecutionContext taskExecutionContext) throws Exception {

    }
}
