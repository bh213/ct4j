package com.whiletrue.ct4j.factory;

import com.whiletrue.ct4j.inmemory.DefaultConstructorTaskFactory;
import com.whiletrue.ct4j.tasks.NoDefaultConstructorTestTask;
import com.whiletrue.ct4j.tasks.NoOpTestTask;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Test default constructor task factory")
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class TestBasicFactory {

    @Test
    @DisplayName("simple task creation")
    public void createTask() throws Exception {
        DefaultConstructorTaskFactory factory = new DefaultConstructorTaskFactory();
        final var task = factory.createInstance(NoOpTestTask.class);
        assertThat(task)
                .isNotNull()
                .isInstanceOf(NoOpTestTask.class);
    }

    @Test
    @DisplayName("task creation failure (task with no default constructor)")
    public void createFailsOnTaskWithNoDefaultConstructor() throws Exception {
        var factory = new DefaultConstructorTaskFactory();
        assertThatThrownBy( ()-> factory.createInstance(NoDefaultConstructorTestTask.class)).isInstanceOf(NoSuchMethodException.class);
    }

    @Test
    @DisplayName("task creation by custom factory")
    public void createWithCustomFactory() throws Exception {
        var factory = new DefaultConstructorTaskFactory();
        factory.addCustomTaskFactory(taskClass -> taskClass == NoDefaultConstructorTestTask.class ?  new NoDefaultConstructorTestTask("custom factory") : null);

        final var task = factory.createInstance(NoDefaultConstructorTestTask.class);
        assertThat(task)
                .isNotNull()
                .isInstanceOf(NoDefaultConstructorTestTask.class)
                .hasFieldOrPropertyWithValue("argument", "custom factory");
    }

    @Test
    @DisplayName("custom factory doesn't provide instance")
    public void testListenerNotProvidingInstance() throws Exception {
        var factory = new DefaultConstructorTaskFactory();
        factory.addCustomTaskFactory(taskClass -> null);
        assertThatThrownBy( ()-> factory.createInstance(NoDefaultConstructorTestTask.class)).isInstanceOf(NoSuchMethodException.class);
    }
}

