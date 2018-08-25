package com.nextbreakpoint.shop.common.model;

public class MessageReceipt {
    private final int status;
    private final long timestamp;

    public MessageReceipt(int status, long timestamp) {
        this.status = status;
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
