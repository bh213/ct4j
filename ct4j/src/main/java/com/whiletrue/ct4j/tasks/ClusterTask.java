package com.whiletrue.ct4j.tasks;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ClusterTask {
    String name();
    int defaultPriority() default -1;

    int maxRetries() default -1;
    int retryDelay() default -1;
    float retryBackoffFactor() default -1;
}
