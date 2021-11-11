package com.nextbreakpoint.blueprint.common.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class KafkaRecord {
    private final String key;
    private final String value;

    @JsonCreator
    public KafkaRecord(
        @JsonProperty("key") String key,
        @JsonProperty("value") String value
    ) {
        this.key = Objects.requireNonNull(key);
        this.value = Objects.requireNonNull(value);
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
