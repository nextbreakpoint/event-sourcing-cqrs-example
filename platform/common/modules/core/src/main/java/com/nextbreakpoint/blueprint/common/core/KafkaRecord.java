package com.nextbreakpoint.blueprint.common.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.Objects;

public class KafkaRecord {
    private final String key;
    private final Map<String, Object> value;

    @JsonCreator
    public KafkaRecord(
        @JsonProperty("key") String key,
        @JsonProperty("value") Map<String, Object> value
    ) {
        this.key = Objects.requireNonNull(key);
        this.value = Objects.requireNonNull(value);
    }

    public String getKey() {
        return key;
    }

    public Map<String, Object> getValue() {
        return value;
    }
}
