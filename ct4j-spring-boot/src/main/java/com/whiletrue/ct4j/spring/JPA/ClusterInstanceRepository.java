package com.whiletrue.ct4j.spring.JPA;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface ClusterInstanceRepository extends JpaRepository<ClusterInstanceEntity, Long> {

    @Modifying
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Query("DELETE FROM ClusterInstanceEntity ci WHERE ci.uniqueRequestId = ?1")
    int deleteByUniqueRequestId(String uniqueRequestId);
}