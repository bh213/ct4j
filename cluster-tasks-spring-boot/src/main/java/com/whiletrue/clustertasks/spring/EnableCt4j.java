package com.whiletrue.clustertasks.spring;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({Ct4jSpringBeanFactory.class})
@Documented
public @interface EnableCt4j {
}
