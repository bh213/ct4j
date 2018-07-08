package com.whiletrue.clustertasks.JPA;

import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({ClusterTasksSpring.class})
@Documented
public @interface EnableClusterTasks {
}
