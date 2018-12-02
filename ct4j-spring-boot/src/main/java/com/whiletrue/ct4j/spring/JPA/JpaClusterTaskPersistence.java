package com.whiletrue.ct4j.spring.JPA;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whiletrue.ct4j.factory.TaskFactory;
import com.whiletrue.ct4j.instanceid.ClusterInstanceNaming;
import com.whiletrue.ct4j.tasks.*;
import com.whiletrue.ct4j.tasks.recurring.RecurringSchedule;
import com.whiletrue.ct4j.tasks.recurring.RecurringScheduleStrategy;
import com.whiletrue.ct4j.timeprovider.TimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class JpaClusterTaskPersistence extends TaskPersistenceBase {

    private static Logger log = LoggerFactory.getLogger(JpaClusterTaskPersistence.class);
    private final ClusterTaskRepository clusterTaskRepository;
    private final ClusterNodePersistence clusterNodePersistence;
    private final ClusterInstanceNaming clusterInstanceNaming;
    private final TaskFactory taskFactory;
    private final ClusterTasksConfig clusterTasksConfig;
    private final TimeProvider timeProvider;

    private ObjectMapper mapper = new ObjectMapper();

    @Autowired
    public JpaClusterTaskPersistence(ClusterTaskRepository clusterTaskRepository, ClusterNodePersistence clusterNodePersistence, ClusterInstanceNaming clusterInstanceNaming, TaskFactory taskFactory, ClusterTasksConfig clusterTasksConfig, TimeProvider timeProvider) {
        super(log, timeProvider, clusterTasksConfig);
        this.clusterTaskRepository = clusterTaskRepository;
        this.clusterNodePersistence = clusterNodePersistence;
        this.clusterInstanceNaming = clusterInstanceNaming;
        this.taskFactory = taskFactory;
        this.clusterTasksConfig = clusterTasksConfig;
        this.timeProvider = timeProvider;
    }



    private Long taskKeyToEntityId(String taskId) {
        if (taskId == null) return null;
        return Long.valueOf(taskId);
    }

    private Long getTaskPrimaryKey(TaskWrapper<?> task) {
        return taskKeyToEntityId(task.getTaskExecutionContext().getTaskId());
    }

    private <INPUT> String serializeInput(INPUT input) throws JsonProcessingException {
        return mapper.writeValueAsString(input);
    }

    private <INPUT> INPUT deserializeInput(String inputJson, Class<INPUT> inputClass) throws IOException {
        return mapper.readValue(inputJson, inputClass);
    }


    private <INPUT> TaskWrapper<INPUT> entityToTask(ClusterTaskEntity entity) {
        try {
            Class<? extends Task> taskClass;

            try {
                taskClass = Class.forName(entity.getTaskClass()).asSubclass(Task.class);
            } catch (Exception e) {
                log.error("Could not find Class for {} for task id {}:{}", entity.getTaskClass(), entity.getId(), e);
                throw new RuntimeException(e);
            }

            Task<INPUT> taskInstance;
            try {
                taskInstance = taskFactory.createInstance(taskClass);
            } catch (Exception createInstanceException) {
                log.error("Could not find create instance of class {} for task id {}:{}", entity.getTaskClass(), entity.getId(), createInstanceException);
                throw new RuntimeException(createInstanceException);
            }

            INPUT input = null;
            if (entity.getInputClass() != null) {
                final Class<INPUT> inputClass;
                try {
                    inputClass = (Class<INPUT>) Class.forName(entity.getInputClass());
                } catch (ClassNotFoundException findInputClassException) {
                    log.error("Could not find input class for {} for task id {}:{}", entity.getTaskClass(), entity.getId(), findInputClassException);
                    throw new RuntimeException(findInputClassException);
                }

                try {
                    input = deserializeInput(entity.getInput(), inputClass);
                } catch (IOException deserializeInputException) {
                    log.error("Could not deserialize input {} of input class {} for task id {}:{}", entity.getInput(), entity.getTaskClass(), entity.getId(), deserializeInputException);
                    throw new RuntimeException(deserializeInputException);
                }
            }

            TaskConfig taskConfig;
            try {
                final ClusterTask clusterTaskAnnotation = Utils.getClusterTaskAnnotation(taskClass);
                final TaskConfig.TaskConfigBuilder builder = new TaskConfig.TaskConfigBuilder(clusterTasksConfig, clusterTaskAnnotation, taskClass);
                taskConfig = taskInstance.configureTask(builder);
                if (taskConfig == null) taskConfig = builder.build();

            } catch (Exception e) {
                log.error("Exception in taskConfig for task id {}:{}", entity.getId(), e);
                throw new RuntimeException(e);
            }

            RecurringSchedule recurringSchedule = null;
            try {
                final String recurringScheduleStrategy = entity.getRecurringSchedule();
                if (recurringScheduleStrategy != null && recurringScheduleStrategy.trim().length() > 0) {

                    if (entity.getNextScheduledTime() == null ) {
                        recurringSchedule = RecurringSchedule.createNewRecurringSchedule(RecurringScheduleStrategy.fromString(recurringScheduleStrategy), timeProvider.getCurrent());
                    } else {
                        recurringSchedule = RecurringSchedule.createExistingRecurringSchedule(RecurringScheduleStrategy.fromString(recurringScheduleStrategy), entity.getNextScheduledTime().toInstant());
                    }


                }
            } catch (Exception e) {
                log.error("Exception in taskConfig for task id {}:{}", entity.getId(), e);
                throw new RuntimeException(e);
            }

            TaskExecutionContext taskExecutionContext = new TaskExecutionContext(entity.getRetryCount() == null ? 0 : entity.getRetryCount(), clusterInstanceNaming.getInstanceId(), entity.getId().toString(), entity.getName(), recurringSchedule);
            return new TaskWrapper<>(taskInstance, input, taskExecutionContext, entity.getLastUpdate().toInstant(), taskConfig);
        } catch (RuntimeException runtimeException) {
            clusterTaskRepository.unlockAndChangeTaskStatus(Collections.singletonList(entity.getId()), TaskStatus.Failure, clusterInstanceNaming.getInstanceId(), timeProvider.getCurrentDate());
            throw runtimeException;
        }
    }

    public ClusterTaskEntity createInitialEntityFromTask(Task<?> task, String serializedInput, String taskName) {
        final var entity = new ClusterTaskEntity();
        entity.setName(taskName);
        entity.setInput(serializedInput);
        entity.setTaskClass(task.getClass().getName());
        entity.setLastUpdate(new Date());
        return entity;
    }

    @Override
    public List<TaskWrapper<?>> pollForNextTasks(int maxTasks) throws Exception {
        // TODO: optimize... a ... lot!!!
        return clusterTaskRepository.getAllPending(PageRequest.of(0, maxTasks), timeProvider.getCurrentDate()).stream().map(this::entityToTask).collect(Collectors.toList());
    }

    @Override
    public long countPendingTasks() {
        return clusterTaskRepository.countPendingTasks(timeProvider.getCurrentDate());
    }


    @Override
    public List<TaskWrapper<?>> findClaimedTasks(List<TaskWrapper<?>> tasks) {
        return clusterTaskRepository.findActuallyLocked(tasks.stream().map(this::getTaskPrimaryKey).collect(Collectors.toList()), clusterInstanceNaming.getInstanceId()).stream().map(this::entityToTask).collect(Collectors.toList());
    }

    @Override
    public int tryClaimTasks(List<TaskWrapper<?>> tasks) {
        return clusterTaskRepository.claimTasks(tasks.stream().map(this::getTaskPrimaryKey).collect(Collectors.toList()), clusterInstanceNaming.getInstanceId(), timeProvider.getCurrentDate());
    }


    @Override
    public <INPUT> String queueTask(Task<INPUT> task, INPUT input) throws Exception {
        return doQueueTask(task, input, 0, 0, null);
    }

    @Override
    public <INPUT> String queueTask(Task<INPUT> task, INPUT input, int priority) throws Exception {
        return doQueueTask(task, input, 0, priority, null);
    }

    @Override
    public <INPUT> String queueTaskDelayed(Task<INPUT> task, INPUT input, long startDelayInMilliseconds) throws Exception {

        return doQueueTask(task, input, startDelayInMilliseconds, null, null);
    }

    @Override
    public <INPUT> String queueTaskDelayed(Task<INPUT> task, INPUT input, long startDelayInMilliseconds, int priority) throws Exception {
        return doQueueTask(task, input, startDelayInMilliseconds, priority, null);
    }


    @Override
    protected <INPUT> List<? extends RecurringTaskEntity> findRecurringTasks(Class<? extends Task> taskClass, INPUT input, ScheduledTaskAction scheduledTaskAction, TaskConfig taskConfig) throws Exception{

        var className = taskClass.getName();
        if (scheduledTaskAction.isPerInput()) {
            final String serializedInput = serializeInput(input, taskConfig);
            return clusterTaskRepository.findRecurringTasksWithInput(className, serializedInput );
        }
        else return clusterTaskRepository.findRecurringTasks(className);
    }

    @Override
    protected <INPUT> void updateRecurringTask(RecurringTaskEntity taskEntity, RecurringSchedule recurringSchedule, INPUT input, TaskConfig taskConfig) throws Exception {
         ClusterTaskEntity cte = (ClusterTaskEntity) taskEntity;

        final String newStrategyString = recurringSchedule.getStrategy().toDatabaseString();
        final String serializedInput = serializeInput(input, taskConfig);

        if (!(Objects.equals(cte.getInput(), serializedInput) && cte.getRecurringSchedule().equals(newStrategyString))) {
            log.info("Task {} input and/or schedule was changed, updating", cte.getTaskId());

            clusterTaskRepository.updateRecurringTasks(List.of(cte.getId()), newStrategyString, serializedInput);

        } else  {
            log.info("Task {} input and/or schedule was not changed, no operation performed", cte.getTaskId());
        }
    }

    protected <INPUT> String doQueueTask(Task<INPUT> task, INPUT input, long startDelayInMilliseconds, Integer priority, RecurringSchedule recurringSchedule) throws Exception {
        TaskConfig taskConfig = getTaskConfig(task, clusterTasksConfig);
        return doQueueTask(task, input, startDelayInMilliseconds, priority, recurringSchedule, taskConfig);

    }

    @Override
    protected <INPUT> String doQueueTask(Task<INPUT> task, INPUT input, long startDelayInMilliseconds, Integer priority, RecurringSchedule recurringSchedule, TaskConfig taskConfig) throws Exception {
        final String serializedInput = serializeInput(input, taskConfig);

        if (input != null) {
            try {
                deserializeInput(serializedInput, input.getClass());
            } catch (Exception e) {
                log.error("Task configuration validation: could not deserialize serialized input. Did you forget to add default constructor to task input class?\nTask '{}', input '{}' input class '{}': {}", taskConfig.getTaskName(), input, input.getClass(), e);
                throw new Exception("Task configuration validation: could not deserialize serialized input. Did you forget to add default constructor to task input class?", e);
            }
        }


        final ClusterTaskEntity entity = createInitialEntityFromTask(task, serializedInput, taskConfig.getTaskName());

        entity.setPriority(priority != null ? priority : taskConfig.getPriority());
        // TODO: get date from db?
        entity.setRetryCount(null);
        entity.setInputClass(input != null ? input.getClass().getName() : null);
        entity.setStatus(TaskStatus.Pending);


        if (recurringSchedule != null) {
            entity.setNextRun(Date.from(recurringSchedule.getNextScheduledRun()));
            entity.setNextScheduledTime(Date.from(recurringSchedule.calculateNextScheduledRun(recurringSchedule.getNextScheduledRun())));
            entity.setRecurringSchedule(recurringSchedule.getStrategy().toDatabaseString());
        } else {
            entity.setNextRun(Date.from(timeProvider.getCurrent().plusMillis(startDelayInMilliseconds)));
        }

        final ClusterTaskEntity saved = clusterTaskRepository.save(entity);
        return Long.toString(saved.getId());
    }

    private <INPUT> String serializeInput(INPUT input, TaskConfig taskConfig) throws Exception{
        final String serializedInput;
        try {
            serializedInput = serializeInput(input);
        } catch (JsonProcessingException e) {
            log.error("Could not serialize input for task '{}', input '{}': {}", taskConfig.getTaskName(), input, e);
            throw new Exception(e);
        }
        return serializedInput;
    }


    @Override
    public void deleteTask(String id) {
        deleteTask(taskKeyToEntityId(id));
    }

    public void deleteTask(long id) {
        log.info("Deleting task {}", id);
        clusterTaskRepository.deleteById(id);
    }

    @Override
    public boolean unlockAndChangeStatus(List<TaskWrapper<?>> tasks, TaskStatus status) {
        int count = clusterTaskRepository.unlockAndChangeTaskStatus(tasks.stream().map(this::getTaskPrimaryKey).collect(Collectors.toList()), status, clusterInstanceNaming.getInstanceId(), timeProvider.getCurrentDate());
        return count > 0; // TODO: count == tasks.size()?
    }

    @Override
    public boolean unlockAndMarkForRetry(TaskWrapper<?> task, int retryCount, Instant nextRun) {
        int count = clusterTaskRepository.unlockAndSetRetryCount(getTaskPrimaryKey(task), clusterInstanceNaming.getInstanceId(), retryCount, Date.from(nextRun), timeProvider.getCurrentDate());
        return count > 0;

    }

    @Override
    public boolean  unlockAndMarkForRetryAndSetScheduledNextRun(TaskWrapper<?> task, int retryCount, Instant nextRun, Instant nextScheduledRun) {

        int count = clusterTaskRepository.unlockAndSetRetryCountAndScheduledNextRun(getTaskPrimaryKey(task), clusterInstanceNaming.getInstanceId(), retryCount, Date.from(nextRun), Date.from(nextScheduledRun));
        return count > 0;
    }

    @Override
    public TaskWrapper<?> getTask(String taskId) {
        final ClusterTaskEntity entity = clusterTaskRepository.findById(taskKeyToEntityId(taskId)).orElse(null);
        if (entity == null) return null;
        return entityToTask(entity);
    }

    @Override
    public TaskStatus getTaskStatus(String taskId) {
        final ClusterTaskEntity entity = clusterTaskRepository.findById(taskKeyToEntityId(taskId)).orElse(null);
        if (entity == null) return null;
        return entity.getStatus();
    }


    public void recoverTasks(String instanceId) {
        final List<ClusterTaskEntity> lockedByInstance = clusterTaskRepository.findLockedByInstance(instanceId);
        // TODO: implement this
    }

    @Override
    public boolean isClustered() {
        return clusterNodePersistence != null;
    }


    @Override
    public ClusterNodePersistence getClusterNodePersistence() {
        return clusterNodePersistence;
    }


    @Override
    public boolean isPersistent() {
        return true;
    }
}
