package com.nextbreakpoint.blueprint.common.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class OutputMessage {
    private String key;
    private Payload value;

    @JsonCreator
    public OutputMessage(
        @JsonProperty("key") String key,
        @JsonProperty("value") Payload value
    ) {
        this.key = Objects.requireNonNull(key);
        this.value = Objects.requireNonNull(value);
    }

    public String getKey() {
        return key;
    }

    public Payload getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "[" +
                "key='" + key + '\'' +
                ", value=" + value +
                "]";
    }
}
