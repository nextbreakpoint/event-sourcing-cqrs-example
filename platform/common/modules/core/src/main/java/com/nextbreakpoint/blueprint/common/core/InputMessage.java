package com.nextbreakpoint.blueprint.common.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class InputMessage {
    private String key;
    private Long offset;
    private Long timestamp;
    private Payload value;
    private Tracing trace;

    @JsonCreator
    public InputMessage(
            @JsonProperty("key") String key,
            @JsonProperty("offset") Long offset,
            @JsonProperty("value") Payload value,
            @JsonProperty("trace") Tracing trace,
            @JsonProperty("timestamp") Long timestamp
    ) {
        this.key = Objects.requireNonNull(key);
        this.offset = Objects.requireNonNull(offset);
        this.value = Objects.requireNonNull(value);
        this.trace = Objects.requireNonNull(trace);
        this.timestamp = Objects.requireNonNull(timestamp);
    }

    public String getKey() {
        return key;
    }

    public Long getOffset() {
        return offset;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public Payload getValue() {
        return value;
    }

    public Tracing getTrace() {
        return trace;
    }

    @Override
    public String toString() {
        return "[" +
                "key='" + key + '\'' +
                ", offset=" + offset +
                ", value=" + value +
                ", trace=" + trace +
                ", timestamp=" + timestamp +
                "]";
    }
}
