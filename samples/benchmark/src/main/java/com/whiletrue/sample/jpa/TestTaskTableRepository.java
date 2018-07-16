package com.whiletrue.sample.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TestTaskTableRepository extends JpaRepository<TestTaskTable, Long>{
}
