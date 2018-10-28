package com.whiletrue.clustertasks.scheduler;
import com.whiletrue.clustertasks.tasks.TaskWrapper;

public interface InternalTaskEvents
{
    void taskStarted(TaskWrapper<?> taskWrapper);
    void taskCompleted(TaskWrapper<?> taskWrapper, int retry, float durationMilliseconds);
    void taskError(TaskWrapper<?> taskWrapper, int retry, float  durationMilliseconds);
    void taskFailed(TaskWrapper<?> taskWrapper, int retry);
}
