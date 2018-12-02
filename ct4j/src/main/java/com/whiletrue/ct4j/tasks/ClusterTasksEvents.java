package com.whiletrue.ct4j.tasks;

public interface ClusterTasksEvents{
    default void taskStarted(Task task) {}
    default void taskCompleted(Task task) {}
    default void taskFailed(Task task) {}
    default void taskError(Task task) {}
    default void taskCreationError(Task task) {}
}
