package com.nextbreakpoint.blueprint.designs.model;

import java.util.Objects;

public class DesignChangedNotification {
    private String key;
    private long timestamp;

    public DesignChangedNotification(String key, long timestamp) {
        this.key = Objects.requireNonNull(key);
        this.timestamp = timestamp;
    }

    public String getKey() {
        return key;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
