package com.nextbreakpoint.blueprint.common.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;

import java.util.Objects;

@Data
@JsonPropertyOrder({
        "key",
        "value"
})
@Builder(toBuilder = true)
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

    public static OutputMessage from(String key, Payload value) {
        return new OutputMessage(key, value);
    }

    @Override
    public String toString() {
        return "[" +
                "key='" + key + '\'' +
                ", value=" + value +
                "]";
    }
}
