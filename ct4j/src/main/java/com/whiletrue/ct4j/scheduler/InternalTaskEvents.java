package com.whiletrue.ct4j.scheduler;
import com.whiletrue.ct4j.tasks.TaskWrapper;

public interface InternalTaskEvents
{
    void taskStarted(TaskWrapper<?> taskWrapper);
    void taskCompleted(TaskWrapper<?> taskWrapper, int retry, float durationMilliseconds);
    void taskError(TaskWrapper<?> taskWrapper, int retry, float  durationMilliseconds);
    void taskFailed(TaskWrapper<?> taskWrapper, int retry);
}
