package tasks;

import com.whiletrue.clustertasks.tasks.Task;
import com.whiletrue.clustertasks.tasks.TaskConfig;
import com.whiletrue.clustertasks.tasks.TaskExecutionContext;

public class NoOpTestTask extends Task<String> {

    @Override
    public void run(String s, TaskExecutionContext taskExecutionContext) throws Exception {

    }
}
