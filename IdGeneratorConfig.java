package com.example.idgen;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetAddress;

@Configuration
public class IdGeneratorConfig {

    /**
     * Set these per instance (env vars / K8s StatefulSet ordinal / config server):
     *   ID_DATACENTER_ID=1  ID_WORKER_ID=7
     * If worker-id is -1, it is derived from the hostname hash (OK for dev, NOT collision-proof in prod).
     */
    @Value("${id.datacenter-id:0}")
    private long datacenterId;

    @Value("${id.worker-id:-1}")
    private long workerId;

    @Bean
    public SnowflakeIdGenerator snowflakeIdGenerator() throws Exception {
        long resolvedWorkerId = workerId >= 0 ? workerId : deriveWorkerIdFromHost();
        return new SnowflakeIdGenerator(datacenterId, resolvedWorkerId);
    }

    private long deriveWorkerIdFromHost() throws Exception {
        String host = InetAddress.getLocalHost().getHostName();
        return Math.floorMod(host.hashCode(), 32);
    }
}
