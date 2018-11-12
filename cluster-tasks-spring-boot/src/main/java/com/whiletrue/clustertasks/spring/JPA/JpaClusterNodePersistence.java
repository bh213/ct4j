package com.whiletrue.clustertasks.spring.JPA;

import com.whiletrue.clustertasks.instanceid.ClusterInstance;
import com.whiletrue.clustertasks.instanceid.ClusterInstanceNaming;
import com.whiletrue.clustertasks.instanceid.ClusterInstanceStatus;
import com.whiletrue.clustertasks.tasks.ClusterNodePersistence;
import com.whiletrue.clustertasks.tasks.ClusterTasksConfig;
import com.whiletrue.clustertasks.tasks.ResourceUsage;
import com.whiletrue.clustertasks.timeprovider.TimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class JpaClusterNodePersistence implements ClusterNodePersistence {

    private static Logger log = LoggerFactory.getLogger(JpaClusterNodePersistence.class);
    private final ClusterInstanceRepository clusterInstanceRepository;
    private final ClusterInstanceNaming clusterInstanceNaming;
    private final ClusterTasksConfig clusterTasksConfig;
    private final TimeProvider timeProvider;

    @Autowired
    public JpaClusterNodePersistence(ClusterInstanceRepository clusterInstanceRepository, ClusterInstanceNaming clusterInstanceNaming, ClusterTasksConfig clusterTasksConfig, TimeProvider timeProvider) {

        this.clusterInstanceNaming = clusterInstanceNaming;
        this.clusterInstanceRepository = clusterInstanceRepository;
        this.clusterTasksConfig = clusterTasksConfig;
        this.timeProvider = timeProvider;
    }

    @Override
    public void instanceInitialCheckIn(String uniqueRequestId) {
        final List<ClusterInstanceEntity> all = clusterInstanceRepository.findAll();
        var clusterInstanceEntity = all.stream().filter(x -> x.getInstanceId().equals(clusterInstanceNaming.getInstanceId())).findFirst().orElse(null);
        if (clusterInstanceEntity != null) {
            log.warn("cluster instance restart detected: {}", clusterInstanceEntity.getInstanceId());
            // TODO: restart/resume tasks?
            // TODO: check timestamps if it is still active due to instance id mismatch
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
            log.error("Instance id name collision detected, expected unique request id {} got {}", previousUniqueRequestId, thisInstance.getUniqueRequestId());
            // TODO:
        }

        thisInstance.setLastCheckIn(timeProvider.getCurrentDate());
        thisInstance.setUniqueRequestId(newUniqueRequestId);
        log.trace("JPAClusterTaskPersistence: Instance {} heartbeat {}", thisInstance.getInstanceId(), thisInstance.getUniqueRequestId());

        final List<ClusterInstanceEntity> returnEntries = deleteExpiredInstances(all);
        clusterInstanceRepository.save(thisInstance); // TODO: replace with JPA method
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


}
