package com.whiletrue.clustertasks.scheduler;

import com.whiletrue.clustertasks.tasks.Task;

public interface InternalTaskEvents
{
    void taskStarted(Class<? extends Task> name, String id);
    void taskCompleted(Class<? extends Task> name, String id, int retry, float milliseconds);
    void taskError(Class<? extends Task> name, String id, int retry, float  milliseconds);
    void taskFailed(Class<? extends Task> name, String id, int retry);
}
