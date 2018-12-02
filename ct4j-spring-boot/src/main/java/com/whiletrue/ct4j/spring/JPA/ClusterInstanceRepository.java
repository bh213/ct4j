package com.whiletrue.ct4j.spring.JPA;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClusterInstanceRepository extends JpaRepository<ClusterInstanceEntity, Long> {

    long deleteByUniqueRequestId(String uniqueRequestId);
}