package com.whiletrue.clustertasks.persistence;


import com.whiletrue.clustertasks.factory.TaskFactory;
import com.whiletrue.clustertasks.inmemory.InMemoryTaskPersistence;
import com.whiletrue.clustertasks.instanceid.ClusterInstance;
import com.whiletrue.clustertasks.tasks.ClusterTasksConfigImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;

import java.time.Instant;

import static org.mockito.Mockito.when;


@DisplayName("In-memory persistence tests")
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class TestInMemoryPersistence extends TestPersistenceBase{

    @BeforeEach
    void init() {
        clusterInstance = Mockito.mock(ClusterInstance.class);
        when(clusterInstance.getInstanceId()).thenReturn("myclusterinstance");
        taskFactory = Mockito.mock(TaskFactory.class);
        fixedTimeProvider.setCurrent(Instant.now());
        taskPersistence = new InMemoryTaskPersistence(clusterInstance, taskFactory, new ClusterTasksConfigImpl(), fixedTimeProvider);
    }
}

