package com.nextbreakpoint.blueprint.common.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class Message {
    private String key;
    private long offset;
    private Long timestamp;
    private Payload payload;

    @JsonCreator
    public Message(
        @JsonProperty("key") String key,
        @JsonProperty("offset") long offset,
        @JsonProperty("timestamp") Long timestamp,
        @JsonProperty("payload") Payload payload
    ) {
        this.key = Objects.requireNonNull(key);
        this.offset = offset;
        this.timestamp = Objects.requireNonNull(timestamp);
        this.payload = Objects.requireNonNull(payload);
    }

    public String getKey() {
        return key;
    }

    public long getOffset() {
        return offset;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public Payload getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        return "[" +
                "key='" + key + '\'' +
                ", offset=" + offset +
                ", timestamp=" + timestamp +
                ", payload=" + payload +
                "]";
    }
}
