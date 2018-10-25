package tasks;


import com.whiletrue.clustertasks.tasks.Task;
import com.whiletrue.clustertasks.tasks.TaskConfig;
import com.whiletrue.clustertasks.tasks.TaskExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WrongInputClassTask extends Task<WrongInputClassTask.InputWithoutDefaultConstructor> {
    private static Logger log = LoggerFactory.getLogger(WrongInputClassTask.class);

    @Override
    public TaskConfig configureTask(TaskConfig.TaskConfigBuilder builder) {
        return builder
                .setPriority(1000)
                .setRetryDelay(1000, 2)
                .setMaxRetries(3)
                .estimateResourceUsage(0.01f, 0)
                .build();
    }

    @Override
    public void run(InputWithoutDefaultConstructor input, TaskExecutionContext taskExecutionContext) throws Exception {
        log.info("Running task");

    }

    public static class InputWithoutDefaultConstructor {

        private String foo;

        public String getFoo() {
            return foo;
        }

        public void setFoo(String foo) {
            this.foo = foo;
        }

        public InputWithoutDefaultConstructor(String foo) {
            this.foo = foo;
        }
    }
}

