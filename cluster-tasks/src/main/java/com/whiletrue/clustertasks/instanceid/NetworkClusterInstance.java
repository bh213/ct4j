package com.whiletrue.clustertasks.instanceid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;


public class NetworkClusterInstance implements ClusterInstance {

    private static Logger log = LoggerFactory.getLogger(NetworkClusterInstance.class);
    private String instanceId;

    public NetworkClusterInstance() {
        final String uuid = UUID.randomUUID().toString().replace("-", "");
        try {

            instanceId = InetAddress.getLocalHost().getHostName() + uuid;
        } catch (UnknownHostException e) {
            log.error("Could not get hostname", e);
            instanceId = uuid;
        }
    }

    @Override
    public String getInstanceId() {
        return instanceId;
    }
}
