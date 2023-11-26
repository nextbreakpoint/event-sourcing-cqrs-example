package com.nextbreakpoint.blueprint.common.core;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true, setterPrefix = "with")
public class OutputMessage<T> {
    private final String key;
    private final MessagePayload<T> value;

    public static <T> OutputMessage<T> from(String key, MessagePayload<T> value) {
        return new OutputMessage<>(key, value);
    }

    @Override
    public String toString() {
        return "[" +
                "key='" + key + '\'' +
                ", value=" + value +
                "]";
    }
}
