package com.whiletrue.clustertasks.JPA;

import com.whiletrue.clustertasks.factory.TaskFactoryBase;
import com.whiletrue.clustertasks.tasks.StdTaskRunner;
import com.whiletrue.clustertasks.tasks.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class SpringTaskFactory extends TaskFactoryBase implements ApplicationContextAware{

    private static Logger log = LoggerFactory.getLogger(StdTaskRunner.class);
    private ApplicationContext applicationContext;

    @Autowired
    public SpringTaskFactory(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public synchronized <TASK extends Task> Task createInstance(Class<TASK> taskClass) throws Exception{

        final Task instance = super.createInstance(taskClass);
        if (instance == null) return applicationContext.getAutowireCapableBeanFactory().createBean(taskClass);
        else return instance;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
