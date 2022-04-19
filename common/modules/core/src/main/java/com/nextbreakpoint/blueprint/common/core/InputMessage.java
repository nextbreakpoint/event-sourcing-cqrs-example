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
        "token",
        "value",
        "timestamp"
})
@Builder(toBuilder = true)
public class InputMessage {
    private String key;
    private String token;
    private Payload value;
    private Long timestamp;

    @JsonCreator
    public InputMessage(
            @JsonProperty("key") String key,
            @JsonProperty("token") String token,
            @JsonProperty("value") Payload value,
            @JsonProperty("timestamp") Long timestamp
    ) {
        this.key = Objects.requireNonNull(key);
        this.token = Objects.requireNonNull(token);
        this.value = Objects.requireNonNull(value);
        this.timestamp = Objects.requireNonNull(timestamp);
    }

    @Override
    public String toString() {
        return "[" +
                "key='" + key + '\'' +
                ", token=" + token +
                ", value=" + value +
                ", timestamp=" + timestamp +
                "]";
    }
}
