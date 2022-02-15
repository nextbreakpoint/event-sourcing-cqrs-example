package com.nextbreakpoint.blueprint.common.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.Objects;

public class KafkaRecord {
    private final String key;
    private final Map<String, Object> value;
    private final Map<String, String> headers;

    @JsonCreator
    public KafkaRecord(
        @JsonProperty("key") String key,
        @JsonProperty("value") Map<String, Object> value,
        @JsonProperty("headers") Map<String, String> headers
    ) {
        this.key = Objects.requireNonNull(key);
        this.value = Objects.requireNonNull(value);
        this.headers = Objects.requireNonNull(headers);
    }

    public String getKey() {
        return key;
    }

    public Map<String, Object> getValue() {
        return value;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }
}
