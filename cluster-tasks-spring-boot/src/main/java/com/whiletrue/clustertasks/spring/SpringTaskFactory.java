package com.whiletrue.clustertasks.spring;

import com.whiletrue.clustertasks.factory.TaskFactoryBase;
import com.whiletrue.clustertasks.tasks.StdTaskRunner;
import com.whiletrue.clustertasks.tasks.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.stereotype.Component;

@Component
public class SpringTaskFactory extends TaskFactoryBase {

    private static Logger log = LoggerFactory.getLogger(StdTaskRunner.class);
    private AutowireCapableBeanFactory autowireCapableBeanFactory;

    @Autowired
    public SpringTaskFactory(AutowireCapableBeanFactory autowireCapableBeanFactory) {
        this.autowireCapableBeanFactory = autowireCapableBeanFactory;
    }

    @Override
    public synchronized <TASK extends Task> Task createInstance(Class<TASK> taskClass) throws Exception{

        final Task instance = super.createInstance(taskClass);
        if (instance == null) return autowireCapableBeanFactory.createBean(taskClass);
        else return instance;
    }


}
