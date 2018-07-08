package tasks;

import com.whiletrue.clustertasks.tasks.Task;
import com.whiletrue.clustertasks.tasks.TaskConfig;
import com.whiletrue.clustertasks.tasks.TaskExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class AutowiredTask extends Task<String> {
    private static Logger log = LoggerFactory.getLogger(AutowiredTask.class);

    private final DummyAutowiredClass autowiredObject;

    @Autowired
    public AutowiredTask(DummyAutowiredClass autowiredObject) {
        this.autowiredObject = autowiredObject;
    }

    @Override
    public TaskConfig configureTask(TaskConfig.TaskConfigBuilder builder) {
        return null;
    }

    @Override
    public void run(String input, TaskExecutionContext taskExecutionContext) throws Exception {
        log.info("Input was {} and autowired was {}", input, autowiredObject);
    }

    public DummyAutowiredClass getAutowiredObject() {
        return autowiredObject;
    }
}
