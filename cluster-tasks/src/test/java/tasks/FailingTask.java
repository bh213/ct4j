package tasks;

import com.whiletrue.clustertasks.tasks.Task;
import com.whiletrue.clustertasks.tasks.TaskExecutionContext;

public class FailingTask extends Task<String> {

    @Override
    public void run(String s, TaskExecutionContext taskExecutionContext) throws Exception {
        throw new Exception("This task always fails");
    }
}
