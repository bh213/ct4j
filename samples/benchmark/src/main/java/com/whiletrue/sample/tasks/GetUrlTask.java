package com.whiletrue.sample.tasks;

import com.whiletrue.ct4j.tasks.Task;
import com.whiletrue.ct4j.tasks.TaskConfig;
import com.whiletrue.ct4j.tasks.TaskExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;


/**
 *  Download content for specific URL, auto-retry
 */

public class GetUrlTask extends Task<GetUrlTask.Input> {
    private static Logger log = LoggerFactory.getLogger(GetUrlTask.class);
    private final RestTemplate restTemplate;

    @Autowired
    public GetUrlTask(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

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
    public void run(Input input, TaskExecutionContext taskExecutionContext) throws Exception {
        log.info("Running GET {}, retry {}", input.getUrl(), taskExecutionContext.getRetry());
        final String response = restTemplate.getForObject(input.url, String.class, taskExecutionContext.getTaskId());
        log.info("Got response, length: {}", Objects.requireNonNull(response).length());

    }

    public static class Input {

        private String url;
        public String getUrl() {
            return url;
        }
        public Input() {
        }

        public Input(String url) {
            this.url = url;
        }
    }
}
