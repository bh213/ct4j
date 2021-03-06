package minimal_jpa_clustered;

import com.whiletrue.ct4j.tasks.ClusterTask;
import com.whiletrue.ct4j.tasks.Task;
import com.whiletrue.ct4j.tasks.TaskExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ClusterTask(name = "example task", maxRetries = 5, retryDelay = 1000, retryBackoffFactor = 1.5f)
public class SampleTask extends Task<String> {
    private static Logger log = LoggerFactory.getLogger(SampleTask.class);

    @Override
    public void run(String input, TaskExecutionContext taskExecutionContext) throws Exception {
        log.info("Task {} called with input {}", taskExecutionContext.getTaskId(), input);
    }
}
