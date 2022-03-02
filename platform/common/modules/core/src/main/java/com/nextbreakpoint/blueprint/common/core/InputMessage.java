package com.nextbreakpoint.blueprint.common.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class InputMessage {
    private String key;
    private Payload value;
    private Tracing trace;
    private Long timestamp;

    @JsonCreator
    public InputMessage(
            @JsonProperty("key") String key,
            @JsonProperty("value") Payload value,
            @JsonProperty("trace") Tracing trace,
            @JsonProperty("timestamp") Long timestamp
    ) {
        this.key = Objects.requireNonNull(key);
        this.value = Objects.requireNonNull(value);
        this.trace = Objects.requireNonNull(trace);
        this.timestamp = Objects.requireNonNull(timestamp);
    }

    public String getKey() {
        return key;
    }

    public Payload getValue() {
        return value;
    }

    public Tracing getTrace() {
        return trace;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "[" +
                "key='" + key + '\'' +
                ", value=" + value +
                ", trace=" + trace +
                ", timestamp=" + timestamp +
                "]";
    }
}
