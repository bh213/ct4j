package com.whiletrue.clustertasks.factory;


import com.whiletrue.clustertasks.inmemory.DefaultConstructorTaskFactory;
import com.whiletrue.clustertasks.tasks.Task;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import com.whiletrue.clustertasks.tasks.NoDefaultConstructorTestTask;
import com.whiletrue.clustertasks.tasks.NoOpTestTask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@DisplayName("Test default constructor task factory")
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class TestBasicFactory {


    @Test
    @DisplayName("Test simple task creation")
    public void createTask() throws Exception {
        DefaultConstructorTaskFactory factory = new DefaultConstructorTaskFactory();
        final Task task = factory.createInstance(NoOpTestTask.class);
        assertThat(task)
                .isNotNull()
                .isInstanceOf(NoOpTestTask.class);
    }

    @Test
    @DisplayName("Test simple task failure")
    public void createFailsOnTaskWithNoDefaultConstructor() throws Exception {
        DefaultConstructorTaskFactory factory = new DefaultConstructorTaskFactory();
        assertThatThrownBy( ()-> factory.createInstance(NoDefaultConstructorTestTask.class)).hasCauseInstanceOf(NoSuchMethodException.class);
    }

    @Test
    @DisplayName("Test creation by custom factory")
    public void createWithCustomFactory() throws Exception {
        DefaultConstructorTaskFactory factory = new DefaultConstructorTaskFactory();
        factory.addCustomTaskFactory(taskClass -> taskClass == NoDefaultConstructorTestTask.class ?  new NoDefaultConstructorTestTask("custom factory") : null);

        final Task task = factory.createInstance(NoDefaultConstructorTestTask.class);
        assertThat(task)
                .isNotNull()
                .isInstanceOf(NoDefaultConstructorTestTask.class)
                .hasFieldOrPropertyWithValue("argument", "custom factory");
    }

    @Test
    @DisplayName("custom factory doesn't provide instance")
    public void testListenerNotProvidingInstance() throws Exception {
        DefaultConstructorTaskFactory factory = new DefaultConstructorTaskFactory();
        factory.addCustomTaskFactory(taskClass -> null);
        assertThatThrownBy( ()-> factory.createInstance(NoDefaultConstructorTestTask.class)).hasCauseInstanceOf(NoSuchMethodException.class);
    }
}

