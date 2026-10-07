package com.example.idgen;

/**
 * Twitter-Snowflake style ID generator.
 * Layout (64 bits): 1 sign | 41 timestamp (ms) | 5 datacenter | 5 worker | 12 sequence
 * -> ~69 years of IDs, 32 DCs x 32 workers, 4096 IDs/ms per node, roughly time-sortable.
 */
public class SnowflakeIdGenerator {

    private static final long CUSTOM_EPOCH = 1735689600000L; // 2025-01-01T00:00:00Z

    private static final long DATACENTER_BITS = 5L;
    private static final long WORKER_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;

    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_BITS);
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_BITS);
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    private static final long WORKER_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_SHIFT = SEQUENCE_BITS + WORKER_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_BITS + DATACENTER_BITS;

    private static final long MAX_CLOCK_BACKWARD_WAIT_MS = 5L;

    private final long datacenterId;
    private final long workerId;

    private long lastTimestamp = -1L;
    private long sequence = 0L;

    public SnowflakeIdGenerator(long datacenterId, long workerId) {
        if (datacenterId < 0 || datacenterId > MAX_DATACENTER_ID) {
            throw new IllegalArgumentException("datacenterId must be between 0 and " + MAX_DATACENTER_ID);
        }
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException("workerId must be between 0 and " + MAX_WORKER_ID);
        }
        this.datacenterId = datacenterId;
        this.workerId = workerId;
    }

    public synchronized long nextId() {
        long timestamp = currentTime();

        if (timestamp < lastTimestamp) {
            long offset = lastTimestamp - timestamp;
            if (offset <= MAX_CLOCK_BACKWARD_WAIT_MS) {
                timestamp = waitUntil(lastTimestamp); // small drift (e.g. NTP): just wait
            } else {
                throw new IllegalStateException("Clock moved backwards by " + offset + " ms");
            }
        }

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {                 // sequence exhausted in this ms
                timestamp = waitUntil(lastTimestamp + 1);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - CUSTOM_EPOCH) << TIMESTAMP_SHIFT)
                | (datacenterId << DATACENTER_SHIFT)
                | (workerId << WORKER_SHIFT)
                | sequence;
    }

    private long waitUntil(long target) {
        long ts = currentTime();
        while (ts < target) {
            Thread.onSpinWait();
            ts = currentTime();
        }
        return ts;
    }

    private long currentTime() {
        return System.currentTimeMillis();
    }
}
