package com.nextbreakpoint.blueprint.common.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.Objects;

@Data
@Builder(toBuilder = true)
public class OutputMessage {
    private String key;
    private Payload value;
    private Tracing trace;

    @JsonCreator
    public OutputMessage(
        @JsonProperty("key") String key,
        @JsonProperty("value") Payload value,
        @JsonProperty("trace") Tracing trace
    ) {
        this.key = Objects.requireNonNull(key);
        this.value = Objects.requireNonNull(value);
        this.trace = Objects.requireNonNull(trace);
    }

    public static OutputMessage from(String key, Payload value, Tracing trace) {
        return new OutputMessage(key, value, trace);
    }

    @Override
    public String toString() {
        return "[" +
                "key='" + key + '\'' +
                ", value=" + value +
                ", trace=" + trace +
                "]";
    }
}
