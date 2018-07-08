package com.whiletrue.clustertasks.scheduler;

public class ExecutionStats {

    private long executions;
    private long errors;
    private long success;
    private long failures;
    public ExecutionStats() {
    }
    public ExecutionStats(long executions, long errors, long success, long failures) {
        this.executions = executions;
        this.errors = errors;
        this.success = success;
        this.failures = failures;
    }

    public ExecutionStats(ExecutionStats copySrc) {
        this.errors = copySrc.errors;
        this.executions = copySrc.executions;
        this.failures = copySrc.failures;
        this.success = copySrc.success;
    }


    public void taskCompleted() {
        executions++;
        success++;
    }

    public void taskError() {
        executions++;
        errors++;
    }

    public void taskFailed() {
        failures++;
    }

    public long getExecutions() {
        return executions;
    }

    public long getErrors() {
        return errors;
    }

    public long getSuccess() {
        return success;
    }

    public long getFailures() {
        return failures;
    }
}
