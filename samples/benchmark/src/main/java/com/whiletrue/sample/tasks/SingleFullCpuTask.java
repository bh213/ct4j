package com.whiletrue.sample.tasks;

import com.whiletrue.clustertasks.tasks.Task;
import com.whiletrue.clustertasks.tasks.TaskConfig;
import com.whiletrue.clustertasks.tasks.TaskExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *  Full cpu usage for specified number of millisecond on single thread
 */
public class SingleFullCpuTask extends Task<SingleFullCpuTask.Input> {
    private static Logger log = LoggerFactory.getLogger(SingleFullCpuTask.class);


    @Override
    public TaskConfig configureTask(TaskConfig.TaskConfigBuilder builder) {
        return builder
                .setPriority(1000)
                .setRetryDelay(1000, 2)
                .setMaxRetries(3)
                .estimateResourceUsage(1.00f, 0)
                .build();
    }

    @Override
    public void run(Input input, TaskExecutionContext taskExecutionContext) throws Exception {

        log.info("Running Single CPU task for {} ms", input.durationInMS);

        long uselessCounter = 0;
        final long start = System.nanoTime();
        while(true) {
            for (int i = 0; i < 10000; i++) {
                uselessCounter++;
            }
            final double elapsed = (System.nanoTime() - start) / 1000000.0;
            if (elapsed > input.durationInMS) {
                log.info("Done in {} ms, useless counter = {}", elapsed, uselessCounter);
                break;
            }
        }

    }

    public static class Input {

        private int durationInMS;

        public int getDurationInMS() {
            return durationInMS;
        }

        public Input() {
        }

        public Input(int durationInMS) {
            this.durationInMS = durationInMS;
        }
    }


}
