package com.whiletrue.clustertasks.tasks;

import java.lang.annotation.Annotation;

public class Utils {

    public static ClusterTask getClusterTaskAnnotation(Class clazz){

        return (ClusterTask) clazz.getDeclaredAnnotation(ClusterTask.class);

    }
}
