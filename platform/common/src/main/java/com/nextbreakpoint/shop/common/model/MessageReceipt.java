package com.nextbreakpoint.shop.common.model;

public class MessageReceipt {
    private final long timestamp;

    public MessageReceipt(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
