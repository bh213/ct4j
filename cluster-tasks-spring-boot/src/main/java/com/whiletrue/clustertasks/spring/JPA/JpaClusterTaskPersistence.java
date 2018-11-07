package com.whiletrue.clustertasks.spring.JPA;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whiletrue.clustertasks.factory.TaskFactory;
import com.whiletrue.clustertasks.instanceid.ClusterInstance;
import com.whiletrue.clustertasks.instanceid.ClusterInstanceNaming;
import com.whiletrue.clustertasks.instanceid.ClusterInstanceStatus;
import com.whiletrue.clustertasks.tasks.*;
import com.whiletrue.clustertasks.timeprovider.TimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class JpaClusterTaskPersistence implements TaskPersistence, TaskClusterPersistence {

    private static Logger log = LoggerFactory.getLogger(JpaClusterTaskPersistence.class);
    private final ClusterTaskRepository clusterTaskRepository;
    private final ClusterInstanceRepository clusterInstanceRepository;
    private final ClusterInstanceNaming clusterInstanceNaming;
    private final TaskFactory taskFactory;
    private final ClusterTasksConfig clusterTasksConfig;
    private final TimeProvider timeProvider;

    private ObjectMapper mapper = new ObjectMapper();

    @Autowired
    public JpaClusterTaskPersistence(ClusterTaskRepository clusterTaskRepository, ClusterInstanceRepository clusterInstanceRepository, ClusterInstanceNaming clusterInstanceNaming, TaskFactory taskFactory, ClusterTasksConfig clusterTasksConfig, TimeProvider timeProvider) {
        this.clusterTaskRepository = clusterTaskRepository;
        this.clusterInstanceNaming = clusterInstanceNaming;
        this.taskFactory = taskFactory;
        this.clusterInstanceRepository = clusterInstanceRepository;
        this.clusterTasksConfig = clusterTasksConfig;
        this.timeProvider = timeProvider;
    }


    @Override
    public void instanceInitialCheckIn(String uniqueRequestId) {
        final List<ClusterInstanceEntity> all = clusterInstanceRepository.findAll();
        ClusterInstanceEntity clusterInstanceEntity = all.stream().filter(x -> x.getInstanceId().equals(clusterInstanceNaming.getInstanceId())).findFirst().orElse(null);
        if (clusterInstanceEntity != null) {
            log.warn("Existing cluster instance detected: {}", clusterInstanceEntity.getInstanceId());
            // TODO: restart/resume tasks?
        }
        else {
            log.info("Registering new cluster instance");
            clusterInstanceEntity = new ClusterInstanceEntity();
        }

        clusterInstanceEntity.setInstanceId(clusterInstanceNaming.getInstanceId());
        clusterInstanceEntity.setLastCheckIn(timeProvider.getCurrentDate());
        clusterInstanceEntity.setCheckInIntervalMilliseconds(clusterTasksConfig.getInstanceCheckinTimeInMilliseconds());
        clusterInstanceEntity.setUniqueRequestId(uniqueRequestId);
        clusterInstanceEntity.setStatus(ClusterInstanceStatus.RUNNING);
        clusterInstanceEntity.setAvailableResourcesCpuCoreUsage(0);// TODO: fix me, get data from config and/or runner
        clusterInstanceEntity.setAvailableResourcesMaximumMemoryUsageInMb(0);
        clusterInstanceEntity.setAvailableResourcesCustomResource1(0);
        clusterInstanceEntity.setAvailableResourcesCustomResource2(0);
        clusterInstanceRepository.saveAndFlush(clusterInstanceEntity);
    }

    @Override
    public void instanceFinalCheckOut(String uniqueRequestId) {

        long deleted = clusterInstanceRepository.deleteByUniqueRequestId(uniqueRequestId);
        if (deleted == 1) log.info("Instance {} checkout successful", clusterInstanceNaming.getInstanceId());
        else log.warn("Instance {} checkout failure. Deleted: {}", clusterInstanceNaming.getInstanceId(), deleted);
    }

    @Override
    public List<ClusterInstance> instanceHeartbeat(List<ClusterInstance> previousInstances, String previousUniqueRequestId, String newUniqueRequestId) {



        final List<ClusterInstanceEntity> all = clusterInstanceRepository.findAll();
        final ClusterInstanceEntity thisInstance = all.stream().filter(x -> x.getInstanceId().equals(clusterInstanceNaming.getInstanceId())).findFirst().orElse(new ClusterInstanceEntity());

        if (!thisInstance.getUniqueRequestId().equals(previousUniqueRequestId)) {
            log.error("Name collision detected, expected unique request id {} got {}", previousUniqueRequestId, thisInstance.getUniqueRequestId());
            // TODO:
        }

        thisInstance.setLastCheckIn(timeProvider.getCurrentDate());
        thisInstance.setUniqueRequestId(newUniqueRequestId);
        log.trace("JPAClusterTaskPersistence: Instance {} heartbeat {}", thisInstance.getInstanceId(), thisInstance.getUniqueRequestId());

        final List<ClusterInstanceEntity> returnEntries = deleteExpiredInstances(all);
        clusterInstanceRepository.saveAndFlush(thisInstance); // TODO: replace with JPA method
        return getClusterInstances(returnEntries);
  }



    private List<ClusterInstanceEntity> deleteExpiredInstances(List<ClusterInstanceEntity> all) {
        // any node that is more than checkInFailureIntervalMultiplier * checkInIntervalMilliseconds older than last check-in is considered invalid and will be removed
        final int checkInFailureIntervalMultiplier = clusterTasksConfig.getCheckInFailureIntervalMultiplier();
        final List<ClusterInstanceEntity> expiredInstances =
                all.stream().filter(x -> x.getLastCheckIn().toInstant().plusMillis(x.getCheckInIntervalMilliseconds() * checkInFailureIntervalMultiplier).isBefore(timeProvider.getCurrent()))
                        .collect(Collectors.toList());


        expiredInstances.forEach(clusterInstanceEntity -> {
            log.error("Node {} checkin expired, removing from instance database. Last checkin {}, interval {} ms, factor {}",
                    clusterInstanceEntity.getInstanceId(),
                    clusterInstanceEntity.getLastCheckIn(),
                    clusterInstanceEntity.getCheckInIntervalMilliseconds(),
                    checkInFailureIntervalMultiplier);
        });
        clusterInstanceRepository.deleteAll(expiredInstances);
        all.removeAll(expiredInstances);
        return all;
    }

    @Override
    public List<ClusterInstance> getClusterInstances() {
        final List<ClusterInstanceEntity> all = clusterInstanceRepository.findAll();
        deleteExpiredInstances(all);
        return getClusterInstances(all);
    }

    private List<ClusterInstance> getClusterInstances(List<ClusterInstanceEntity> entities) {
        if (entities == null) return null;
        return entities.stream().map(x->{
            final ResourceUsage resourceUsage = new ResourceUsage(
                    (float)x.getAvailableResourcesCpuCoreUsage(),
                    (float)x.getAvailableResourcesMaximumMemoryUsageInMb(),
                    clusterTasksConfig.getConfiguredResources().getCustomResource1Name(),
                    (float)x.getAvailableResourcesCustomResource1(),
                    clusterTasksConfig.getConfiguredResources().getCustomResource2Name(),
                    (float)x.getAvailableResourcesCustomResource2());

            return new ClusterInstance(x.getInstanceId(),x.getLastCheckIn().toInstant(), x.getCheckInIntervalMilliseconds(), x.getStatus(), x.getCheckStatusRequest(), resourceUsage);
        }).collect(Collectors.toList());
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

            TaskExecutionContext taskExecutionContext = new TaskExecutionContext(entity.getRetryCount() == null ? 0 : entity.getRetryCount(), clusterInstanceNaming.getInstanceId(), entity.getId().toString(), entity.getName());
            return new TaskWrapper(taskInstance, input, taskExecutionContext, entity.getLastUpdate().toInstant(), taskConfig);
        } catch (RuntimeException runtimeException) {
            clusterTaskRepository.unlockAndChangeTaskStatus(Collections.singletonList(entity.getId()), TaskStatus.Failure, clusterInstanceNaming.getInstanceId(), timeProvider.getCurrentDate());
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
        return clusterTaskRepository.getAllPending(PageRequest.of(0, maxTasks), timeProvider.getCurrentDate()).stream().map(this::entityToTask).collect(Collectors.toList());
    }

    @Override
    public long countPendingTasks() {
        return clusterTaskRepository.countPendingTasks(timeProvider.getCurrentDate());
    }

    @Override
    public TaskClusterPersistence getClustered() {
        return this;
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
        int count = clusterTaskRepository.unlockAndChangeTaskStatus(tasks.stream().map(this::getTaskPrimaryKey).collect(Collectors.toList()), status, clusterInstanceNaming.getInstanceId(), timeProvider.getCurrentDate());

    }

    @Override
    public void unlockAndMarkForRetry(TaskWrapper<?> task, int retryCount, Instant newScheduledTime) {
        int count = clusterTaskRepository.unlockAndSetRetryCount(getTaskPrimaryKey(task), clusterInstanceNaming.getInstanceId(), retryCount, Date.from(newScheduledTime), timeProvider.getCurrentDate());

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

    @Override
    public boolean isClustered() {
        return true;
    }


    @Override
    public boolean isPersistent() {
        return true;
    }
}
