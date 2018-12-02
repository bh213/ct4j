package com.whiletrue.ct4j.tasks;

public class Utils {

    public static ClusterTask getClusterTaskAnnotation(Class clazz){

        return (ClusterTask) clazz.getDeclaredAnnotation(ClusterTask.class);

    }
}
