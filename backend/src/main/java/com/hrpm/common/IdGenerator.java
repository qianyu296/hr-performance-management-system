package com.hrpm.common;

import org.springframework.stereotype.Component;

@Component
public class IdGenerator {
    private static final long EPOCH = 1_704_067_200_000L;
    private long lastTimestamp = -1L;
    private int sequence;

    public synchronized long nextId() {
        long timestamp = Math.max(System.currentTimeMillis(), lastTimestamp);
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & 0xFFF;
            if (sequence == 0) {
                do {
                    timestamp = System.currentTimeMillis();
                } while (timestamp <= lastTimestamp);
            }
        } else {
            sequence = 0;
        }
        lastTimestamp = timestamp;
        return ((timestamp - EPOCH) << 12) | sequence;
    }
}
