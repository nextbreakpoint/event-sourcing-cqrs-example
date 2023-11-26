package com.nextbreakpoint.blueprint.common.core;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true, setterPrefix = "with")
public class InputMessage<T> {
    private final String key;
    private final String token;
    private final MessagePayload<T> value;
    private final Long timestamp;

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
