package com.whiletrue.sample;


import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.whiletrue.clustertasks.tasks.StdTaskRunner;
import com.whiletrue.clustertasks.tasks.TaskManager;
import com.whiletrue.sample.jpa.TestTaskTable;
import com.whiletrue.sample.jpa.TestTaskTableRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

@Component
public class SampleRunner implements ApplicationRunner {

    private static Logger log = LoggerFactory.getLogger(SampleRunner.class);
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    private TaskManager taskManager;
    @Autowired
    private TestTaskTableRepository testTaskTableRepository;

    @Override
    public void run(ApplicationArguments args) throws Exception {

        if (!args.containsOption("generator"))
        {
            log.info("Running as node");
            return;
        }

        if (args.containsOption("stop"))
        {
            log.info("Stopping scheduler");
            taskManager.stopScheduling();
        }

        if (args.containsOption("nologs")){
            log.info("Disabling task logs");
            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            loggerContext.getLogger(TestTask.class).setLevel(Level.OFF);
            loggerContext.getLogger(StdTaskRunner.class).setLevel(Level.OFF);
        }

        if (args.containsOption("cleandb")){
            log.info("Cleaning db");
            jdbcTemplate.update("delete from cluster_tasks");
            jdbcTemplate.update("delete from test_task_table");
            log.info("done");
        }

        int count = 10 * 1000;

        if (args.containsOption("generator")) {




            log.info("Testing with count {}", count);

            StopWatch sw = new StopWatch();
            sw.start("creating tasks");

            AtomicInteger taskCounter = new AtomicInteger(0);

            IntStream.range(0, count).parallel().forEach(i -> {

                final int countValue = taskCounter.incrementAndGet();
                if (countValue  % 1000 == 0) {
                    log.info("{}/{}", countValue, count);
                }
                final TestTaskTable save = testTaskTableRepository.save(new TestTaskTable((long) i, i % 3));
                try {
                    taskManager.queueTask(TestTask.class, new TestTask.Input(save.getId(), (int) save.getExpectedRetries()));
                } catch (Exception e) {
                    log.error("Error in queue task", e);
                }

            });
            sw.stop();
            log.info("Created {} tasks in {}", count, sw.getLastTaskInfo().getTimeMillis());
        }

        taskManager.startScheduling();
        waitForEnd();


        List<TestTaskTable> all = testTaskTableRepository.findAll();
        log.info("===============================================================");
        log.info("Rows = {}, Tasks = {}", all.size(), count);


        int invalid = 0;
        int totalExecutions = 0;

        for (TestTaskTable row : all) {
            int retryCount = 0;
            if (row.getRetry1() != null) retryCount++;
            if (row.getRetry2() != null) retryCount++;
            if (row.getRetry3() != null) retryCount++;
            if (retryCount != row.getExpectedRetries()) {
                invalid++;
                continue;
            }
            totalExecutions += retryCount + 1;

            if (row.getError() != null) {
                log.error("Task {} error:{}\n{}", row.getId(), row.getError(), row.getLog());
                invalid++;
                continue;
            }
            if (row.getDone() != true) {
                invalid++;
                continue;
            }
        }

        if (invalid == 0) log.info("No Invalid tasks");
        else log.error("Invalid tasks found: {}", invalid);

        log.info("Total executions = {}", totalExecutions);


      /*  log.info("Creating tables using JDBC templates");
        sw.start("create using JDBC template");



DO
$do$
BEGIN
for i in 1..100000 loop
insert into cluster_tasks (description, input, lock_time, locked_by_instance_id, name, next_run, retry_count, status, task_class, type, id) values ('desc', 'input', null, null, 'name', CURRENT_TIMESTAMP, null, 'Pending', 'classtype', 'type', nextval('cluster_tasks_id_seq'));
end loop;
END
$do$;

       */

        //clusterTaskRepository.deleteAllInBatch();
    }

    private void waitForEnd() throws InterruptedException {
        StopWatch sw = new StopWatch();
        sw.start("execution");



        Instant lastCheck = Instant.now();
        while (true) {
            int rowscount = jdbcTemplate.queryForObject("select count(*) from cluster_tasks where status='Pending' OR status='Running'", Integer.class);

            if (Duration.between(lastCheck, Instant.now()).toMillis() > 1000)
            {
                log.info("---------------------------\n rowcount {}", rowscount);
                log.info(taskManager.getStats());
                log.info("resources = {}", taskManager.getFreeResourcesEstimate());
                lastCheck = Instant.now();
            }



            if (rowscount == 0) break;
            Thread.sleep(300);
        }

        sw.stop();
        log.info("Ran tasks tasks in {}", sw.getLastTaskInfo().getTimeMillis());
    }




}