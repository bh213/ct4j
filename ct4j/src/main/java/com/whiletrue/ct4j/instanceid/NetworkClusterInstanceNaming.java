package com.whiletrue.ct4j.instanceid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;


public class NetworkClusterInstanceNaming implements ClusterInstanceNaming {

    private static Logger log = LoggerFactory.getLogger(NetworkClusterInstanceNaming.class);
    private String instanceId;

    public NetworkClusterInstanceNaming() {
        final String uuid = UUID.randomUUID().toString().replace("-", "");
        try {

            instanceId = InetAddress.getLocalHost().getHostName() + uuid;
        } catch (UnknownHostException e) {
            log.error("Could not get hostname", e);
            instanceId = uuid;
        }

        if (instanceId.length() > INSTANCE_ID_LENGTH) instanceId = instanceId.substring(0, INSTANCE_ID_LENGTH);
    }

    @Override
    public String getInstanceId() {
        return instanceId;
    }
}
