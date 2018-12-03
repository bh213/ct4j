package com.whiletrue.ct4j.instanceid;

public interface ClusterInstanceNaming {
    int INSTANCE_ID_LENGTH = 64;
    String getInstanceId();
}
