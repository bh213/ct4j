package com.whiletrue.clustertasks.factory;


import com.whiletrue.clustertasks.spring.SpringTaskFactory;
import com.whiletrue.clustertasks.tasks.Task;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import com.whiletrue.clustertasks.tasks.AutowiredTask;
import com.whiletrue.clustertasks.tasks.DummyAutowiredClass;
import com.whiletrue.clustertasks.tasks.NoDefaultConstructorTestTask;
import com.whiletrue.clustertasks.tasks.NoOpTestTask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@DisplayName("Test spring boot factory")
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@ExtendWith(SpringExtension.class)
@Configuration
public class TestSpringBootFactory {

    @Autowired
    AutowireCapableBeanFactory applicationContext;


    @Configuration
    public static class TestConfig
    {
        @Bean
        public DummyAutowiredClass getAutowired() {
            return new DummyAutowiredClass("test", "spring boot factory");
        }
    }

    @Test
    @DisplayName("Test simple task creation")
    public void createTask() throws Exception {
        SpringTaskFactory factory = new SpringTaskFactory(applicationContext);
        final Task task = factory.createInstance(NoOpTestTask.class);
        assertThat(task)
                .isNotNull()
                .isInstanceOf(NoOpTestTask.class);
    }

    @Test
    @DisplayName("Test simple autowired tast")
    public void createAutowiredTask() throws Exception {
        SpringTaskFactory factory = new SpringTaskFactory(applicationContext);
        final Task task = factory.createInstance(AutowiredTask.class);
        assertThat(task)
                .isNotNull()
                .isInstanceOf(AutowiredTask.class);
        AutowiredTask autowiredTask = (AutowiredTask) task;
        assertThat(autowiredTask.getAutowiredObject())
                .isNotNull()
                .hasFieldOrPropertyWithValue("name", "test")
                .hasFieldOrPropertyWithValue("value", "spring boot factory");
    }


    @Test
    @DisplayName("Test simple task failure")
    public void createFailsOnTaskWithNoDefaultConstructor() throws Exception {
        SpringTaskFactory factory = new SpringTaskFactory(applicationContext);
        assertThatThrownBy( ()-> factory.createInstance(NoDefaultConstructorTestTask.class)).hasCauseInstanceOf(NoSuchBeanDefinitionException.class);

    }


    @Test
    @DisplayName("Test creation by custom factory")
    public void createWithCustomFactory() throws Exception {
        SpringTaskFactory factory = new SpringTaskFactory(applicationContext);
        factory.addCustomTaskFactory(taskClass -> taskClass == NoDefaultConstructorTestTask.class ? new NoDefaultConstructorTestTask("custom factory") : null);

        final Task task = factory.createInstance(NoDefaultConstructorTestTask.class);
        assertThat(task)
                .isNotNull()
                .isInstanceOf(NoDefaultConstructorTestTask.class)
                .hasFieldOrPropertyWithValue("argument", "custom factory");
    }

    @Test
    @DisplayName("Custom factory doesn't provide instance")
    public void testListenerNotProvidingInstance() throws Exception {
        SpringTaskFactory factory = new SpringTaskFactory(applicationContext);
        factory.addCustomTaskFactory(taskClass -> null);

        assertThatThrownBy( ()-> factory.createInstance(NoDefaultConstructorTestTask.class)).hasCauseInstanceOf(NoSuchBeanDefinitionException.class);
    }
}

