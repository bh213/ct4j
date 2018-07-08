package com.whiletrue.sample;

import com.whiletrue.clustertasks.tasks.Task;
import com.whiletrue.clustertasks.tasks.TaskConfig;
import com.whiletrue.clustertasks.tasks.TaskExecutionContext;
import com.whiletrue.clustertasks.tasks.TaskManager;
import com.whiletrue.sample.jpa.TestTaskTable;
import com.whiletrue.sample.jpa.TestTaskTableRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;


public class TestTask extends Task<TestTask.Input> {
    private static Logger log = LoggerFactory.getLogger(TestTask.class);

    private TaskManager taskManager;

    private ApplicationContext applicationContext;

     private TestTaskTableRepository testTaskTableRepository;

   @Autowired
    public TestTask(TaskManager taskManager, ApplicationContext applicationContext) {
        this.taskManager = taskManager;
       this.applicationContext = applicationContext;
       this.testTaskTableRepository = (TestTaskTableRepository) applicationContext.getBean("testTaskTableRepository");
    }


    @Override
    public TaskConfig configureTask(TaskConfig.TaskConfigBuilder builder) {
        return builder
                .setPriority(1000)
                .setRetryDelay(0, 1)
                .setMaxRetries(10)
                .estimateResourceUsage(0.05f, 10)
                .build();
    }

    @Override
    public void run(Input input, TaskExecutionContext taskExecutionContext) throws Exception {

        log.info("running row {} retry {}, requested retries {}", input.rowid, taskExecutionContext.getRetry(), input.retries);

        Thread.sleep(300);
        final TestTaskTable table = testTaskTableRepository.findOne(input.rowid);
        if (table == null) throw new Exception("No table for me!");



        //if (table.getId() == 444) throw new RuntimeException("Waaa");

        table.setInstanceId(taskExecutionContext.getClusterNodeId());
        table.setLog(table.getLog() + "\n" + taskExecutionContext.getClusterNodeId() + " retry" + taskExecutionContext.getRetry() + " total retries " + input.retries);


        switch (taskExecutionContext.getRetry()) {
            case 0:
                break;
            case 1: {
                if (table.getRetry1() != null) {
                    table.setError("retry 1 already set");
                } else table.setRetry1(true);
                break;
            }
            case 2: {
                if (table.getRetry2() != null) {
                    table.setError("retry 2 already set");
                } else table.setRetry2(true);
                break;
            }
            case 3: {
                if (table.getRetry3() != null) {
                    table.setError("retry 3 already set");
                } else table.setRetry3(true);
                break;
            }
            default:
                table.setError("unexpected retry " + taskExecutionContext.getRetry());
        }



        if (taskExecutionContext.getRetry() < input.retries) {
            testTaskTableRepository.save(table);
            throw new Exception("Requested retry");
        }

        table.setDone(true);
        testTaskTableRepository.save(table);

    }

    public static class Input {

        public long rowid;
        public int retries;

        public Input() {
        }

        public Input(long rowid, int retries) {
            this.rowid = rowid;
            this.retries = retries;
        }
    }


}
