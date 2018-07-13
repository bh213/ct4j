package com.whiletrue.clustertasks.spring.JPA;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whiletrue.clustertasks.factory.TaskFactory;
import com.whiletrue.clustertasks.instanceid.ClusterInstance;
import com.whiletrue.clustertasks.tasks.*;
import com.whiletrue.clustertasks.timeprovider.TimeProvider;
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
public class JpaClusterTaskPersistence implements TaskPersistence {

    private static Logger log = LoggerFactory.getLogger(JpaClusterTaskPersistence.class);
    private final ClusterTaskRepository clusterTaskRepository;
    private final ClusterInstanceRepository clusterInstanceRepository;
    private final ClusterInstance clusterInstance;
    private final TaskFactory taskFactory;
    private final ClusterTasksConfig clusterTasksConfig;
    private final TimeProvider timeProvider;

    private ObjectMapper mapper = new ObjectMapper();

    @Autowired
    public JpaClusterTaskPersistence(ClusterTaskRepository clusterTaskRepository, ClusterInstanceRepository clusterInstanceRepository, ClusterInstance clusterInstance, TaskFactory taskFactory, ClusterTasksConfig clusterTasksConfig, TimeProvider timeProvider) {
        this.clusterTaskRepository = clusterTaskRepository;
        this.clusterInstance = clusterInstance;
        this.taskFactory = taskFactory;
        this.clusterInstanceRepository = clusterInstanceRepository;
        this.clusterTasksConfig = clusterTasksConfig;
        this.timeProvider = timeProvider;
    }


    public void instanceCheckIn() {
        final List<ClusterInstanceEntity> all = clusterInstanceRepository.findAll();
        final ClusterInstanceEntity thisInstance = all.stream().filter(x -> x.getInstanceId().equals(clusterInstance.getInstanceId())).findFirst().orElse(new ClusterInstanceEntity());
        thisInstance.setInstanceId(clusterInstance.getInstanceId());
        thisInstance.setLastCheckIn(new Date());
        thisInstance.setCheckInIntervalMilliseconds(9999); // TODO
        // TODO: count valid instances
    }


    private Long taskKeyToEntityId(String taskId) {
        if (taskId == null) return null;
        return Long.valueOf(taskId);
    }

    private Long getTaskPrimaryKey(TaskWrapper<?> task) {
        return taskKeyToEntityId(task.getTaskExecutionContext().getTaskId());
    }

    <INPUT> String serializeInput(INPUT input) throws JsonProcessingException {
        return mapper.writeValueAsString(input);
    }

    <INPUT> INPUT deserializeInput(String inputJson, Class<INPUT> inputClass) throws IOException {
        return mapper.readValue(inputJson, inputClass);
    }


    public <INPUT> TaskWrapper<INPUT> entityToTask(ClusterTaskEntity entity) {
        try {
            Class taskClass = null;

            try {
                taskClass = Class.forName(entity.getTaskClass());
            } catch (ClassNotFoundException e) {
                log.error("Could not find Class for {} for task id {}:{}", entity.getTaskClass(), entity.getId(), e);
                throw new RuntimeException(e);
            }

            Task taskInstance = null;
            try {
                taskInstance = taskFactory.createInstance(taskClass);
            } catch (Exception createInstanceException) {
                log.error("Could not find create instance of class {} for task id {}:{}", entity.getTaskClass(), entity.getId(), createInstanceException);
                throw new RuntimeException(createInstanceException);
            }

            Object input = null;
            if (entity.getInputClass() != null) {
                final Class inputClass;
                try {
                    inputClass = Class.forName(entity.getInputClass());
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

            TaskExecutionContext taskExecutionContext = new TaskExecutionContext(entity.getRetryCount() == null ? 0 : entity.getRetryCount(), clusterInstance.getInstanceId(), entity.getId().toString(), entity.getName());
            return new TaskWrapper(taskInstance, input, taskExecutionContext, entity.getLastUpdate().toInstant(), taskConfig);
        } catch (RuntimeException runtimeException) {
            clusterTaskRepository.unlockAndChangeTaskStatus(Collections.singletonList(entity.getId()), TaskStatus.Failure, clusterInstance.getInstanceId(), timeProvider.getCurrentDate());
            throw runtimeException;
        }
    }

    public ClusterTaskEntity createInitialEntityFromTask(Task<?> task, String serializedInput, String taskName) {
        final ClusterTaskEntity entity = new ClusterTaskEntity();
        entity.setName(taskName);
        entity.setInput(serializedInput);
        entity.setTaskClass(task.getClass().getName());
        entity.setLastUpdate(new Date());

        return entity;

    }

    @Override
    public List<TaskWrapper<?>> pollForNextTasks(int maxTasks) throws Exception {
        // TODO: optimize... a ... lot!!!
        return clusterTaskRepository.getAllPending(new PageRequest(0, maxTasks), timeProvider.getCurrentDate()).stream().map(this::entityToTask).collect(Collectors.toList());
    }

    @Override
    public List<TaskWrapper<?>> findClaimedTasks(List<TaskWrapper<?>> tasks) {
        return clusterTaskRepository.findActuallyLocked(tasks.stream().map(this::getTaskPrimaryKey).collect(Collectors.toList()), clusterInstance.getInstanceId()).stream().map(entity -> entityToTask(entity)).collect(Collectors.toList());
    }

    @Override
    public int tryClaimTasks(List<TaskWrapper<?>> tasks) {
        final int count = clusterTaskRepository.claimTasks(tasks.stream().map(this::getTaskPrimaryKey).collect(Collectors.toList()), clusterInstance.getInstanceId(), timeProvider.getCurrentDate());
        return count;
    }


    @Override
    public <INPUT> String queueTask(Task<INPUT> task, INPUT input) throws Exception {
        return doQueueTask(task, input, 0, 0);
    }

    @Override
    public <INPUT> String queueTask(Task<INPUT> task, INPUT input, int priority) throws Exception {
        return doQueueTask(task, input, 0, priority);
    }

    @Override
    public <INPUT> String queueTaskDelayed(Task<INPUT> task, INPUT input, long startDelayInMilliseconds) throws Exception {

        return doQueueTask(task, input, startDelayInMilliseconds, null);
    }

    @Override
    public <INPUT> String queueTaskDelayed(Task<INPUT> task, INPUT input, long startDelayInMilliseconds, int priority) throws Exception {
        return doQueueTask(task, input, startDelayInMilliseconds, priority);
    }


    private <INPUT> String doQueueTask(Task<INPUT> task, INPUT input, long startDelayInMilliseconds, Integer priority) throws Exception {
        final Class<? extends Task> taskClass = Objects.requireNonNull(task).getClass();
        final ClusterTask clusterTaskAnnotation = Utils.getClusterTaskAnnotation(taskClass);
        final TaskConfig.TaskConfigBuilder builder = new TaskConfig.TaskConfigBuilder(clusterTasksConfig, clusterTaskAnnotation, taskClass);
        TaskConfig taskConfig = task.configureTask(builder);
        if (taskConfig == null) taskConfig = builder.build();


        final String serializedInput;
        try {
            serializedInput = serializeInput(input);
        } catch (JsonProcessingException e) {
            log.error("Could not serialize input for task '{}', input '{}': {}", taskConfig.getTaskName(), input, e);
            throw new Exception(e);
        }

        final ClusterTaskEntity entity = createInitialEntityFromTask(task, serializedInput, taskConfig.getTaskName());

        entity.setPriority(priority != null ? priority : taskConfig.getPriority());
        // TODO: get date from db?
        entity.setRetryCount(null);
        entity.setInputClass(input != null ? input.getClass().getName() : null);
        entity.setStatus(TaskStatus.Pending);
        entity.setNextRun(Date.from(timeProvider.getCurrent().plusMillis(startDelayInMilliseconds)));
        final ClusterTaskEntity saved = clusterTaskRepository.save(entity);
        return Long.toString(saved.getId());
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
    public void unlockAndChangeStatus(List<TaskWrapper<?>> tasks, TaskStatus status) {
        int count = clusterTaskRepository.unlockAndChangeTaskStatus(tasks.stream().map(this::getTaskPrimaryKey).collect(Collectors.toList()), status, clusterInstance.getInstanceId(), timeProvider.getCurrentDate());

    }

    @Override
    public void unlockAndMarkForRetry(TaskWrapper<?> task, int retryCount, Instant newScheduledTime) {
        int count = clusterTaskRepository.unlockAndSetRetryCount(getTaskPrimaryKey(task), clusterInstance.getInstanceId(), retryCount, Date.from(newScheduledTime), timeProvider.getCurrentDate());

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

}
