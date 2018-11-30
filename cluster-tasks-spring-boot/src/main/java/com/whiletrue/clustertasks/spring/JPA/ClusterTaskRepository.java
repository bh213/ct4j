package com.whiletrue.clustertasks.spring.JPA;

import com.whiletrue.clustertasks.tasks.TaskStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Date;
import java.util.List;

@Repository
public interface ClusterTaskRepository extends JpaRepository<ClusterTaskEntity, Long> {

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Modifying
    @Query("UPDATE ClusterTaskEntity ct SET ct.lockedByInstanceId = ?2, ct.lastUpdate = ?3, ct.lockTime = ?3, ct.status='Running' WHERE ct.id IN ?1 AND ct.lockedByInstanceId = NULL AND ct.status='Pending'")
    int claimTasks(Collection<Long> id, String instanceId, Date current);

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Modifying
    @Query("UPDATE ClusterTaskEntity ct SET ct.lockedByInstanceId = NULL, ct.lastUpdate = ?2, ct.lockTime = NULL WHERE ct.id=?1")
    long breakLock(Long id, Date current);


    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Query("SELECT ct FROM ClusterTaskEntity ct WHERE ct.status='Pending' AND ct.lockTime = NULL AND ct.nextRun <= ?1 ORDER BY ct.priority DESC")
    List<ClusterTaskEntity> getAllPending(Pageable pageable, Date current);

    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    @Query("SELECT COUNT(ct) FROM ClusterTaskEntity ct WHERE ct.status='Pending' AND ct.lockTime = NULL AND ct.nextRun <= ?1 ")
    long  countPendingTasks(Date current);

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Query("SELECT ct FROM ClusterTaskEntity ct WHERE ct.lockedByInstanceId = ?2 AND ct.id IN ?1")
    List<ClusterTaskEntity> findActuallyLocked(Collection<Long> id, String instanceId);

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Modifying
    @Query("UPDATE ClusterTaskEntity ct SET ct.lockedByInstanceId = NULL, ct.lastUpdate = ?4, ct.lockTime = NULL, ct.status=?2 WHERE ct.id IN ?1 AND ct.lockedByInstanceId = ?3 ")
    int unlockAndChangeTaskStatus(Collection<Long> id, TaskStatus status, String instanceId, Date current);

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Modifying
    @Query("UPDATE ClusterTaskEntity ct SET ct.lockedByInstanceId = NULL, ct.lastUpdate = ?5, ct.lockTime = NULL, ct.status='Pending', ct.retryCount=?3, ct.nextRun=?4 WHERE ct.id =?1 AND ct.lockedByInstanceId = ?2")
    int unlockAndSetRetryCount(long id, String instanceId, int retryCount, Date newScheduledTime, Date current);

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Modifying
    @Query("UPDATE ClusterTaskEntity ct SET ct.lockedByInstanceId = NULL, ct.lastUpdate = ?5, ct.lockTime = NULL, ct.status='Pending', ct.retryCount=?3, ct.nextRun=?4, ct.nextScheduledTime=?5 WHERE ct.id =?1 AND ct.lockedByInstanceId = ?2")
    int unlockAndSetRetryCountAndScheduledNextRun(Long taskPrimaryKey, String instanceId, int retryCount, Date from, Date currentDate);

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Query("SELECT ct FROM ClusterTaskEntity ct WHERE ct.lockedByInstanceId = ?1 AND ct.lockTime IS NOT NULL")
    List<ClusterTaskEntity> findLockedByInstance(String instanceId);

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Query("SELECT ct FROM ClusterTaskEntity ct WHERE ct.taskClass = ?1 AND ct.input = ?2 AND ct.recurringSchedule IS NOT NULL")
    List<ClusterTaskEntity> findRecurringTasksWithInput(String taskClass, String input);

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Query("SELECT ct FROM ClusterTaskEntity ct WHERE ct.taskClass = ?1  AND ct.recurringSchedule IS NOT NULL")
    List<ClusterTaskEntity> findRecurringTasks(String taskClass);


    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Modifying
    @Query("UPDATE ClusterTaskEntity ct SET ct.recurringSchedule = ?2, ct.input= ?3 WHERE ct.id IN ?1")
    int updateRecurringTasks(Collection<Long> id, String recurringSchedule, String input);


}