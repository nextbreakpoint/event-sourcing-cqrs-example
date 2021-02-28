package com.nextbreakpoint.blueprint.common.core;

public class MessageReceipt {
    private final long timestamp;

    public MessageReceipt(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
