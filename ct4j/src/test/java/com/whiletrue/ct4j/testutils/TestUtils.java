package com.whiletrue.ct4j.testutils;

import com.whiletrue.ct4j.tasks.*;

import java.time.Instant;
import java.util.function.Consumer;

public class TestUtils {

    public static <INPUT> TaskWrapper<INPUT> createTaskWrapper(Task<INPUT> task, INPUT input, int retry, String clusterNodeId, String taskId, String taskName, ClusterTasksConfig config) {
        return createTaskWrapper(task, input, retry, clusterNodeId, taskId, taskName, config, null);
    }

    public static <INPUT> TaskWrapper<INPUT> createTaskWrapper(Task<INPUT> task, INPUT input, int retry, String clusterNodeId, String taskId, String taskName, ClusterTasksConfig config, Consumer<TaskConfig.TaskConfigBuilder> builderAction){


        final TaskConfig.TaskConfigBuilder builder = new TaskConfig.TaskConfigBuilder(config, null, task.getClass());
        if (builderAction != null) builderAction.accept(builder);

        TaskWrapper<INPUT> wrapper = new TaskWrapper<INPUT>(task, input, new TaskExecutionContext(retry, clusterNodeId, taskId,taskName, null ), Instant.now(), builder.build());
        return wrapper;
    }

}
