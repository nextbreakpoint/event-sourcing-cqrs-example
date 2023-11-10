package com.nextbreakpoint.blueprint.common.core;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder(setterPrefix = "with")
public class MessagePayload<T> {
    private final UUID uuid;
    private final String type;
    private final String source;
    private final T data;

    public UUID getUuid() {
        return uuid;
    }

    public String getType() {
        return type;
    }

    public String getSource() {
        return source;
    }

    public T getData() {
        return data;
    }

    @Override
    public String toString() {
        return "[" +
                "uuid=" + uuid +
                ", type='" + type + '\'' +
                ", source='" + source + '\'' +
                ", data='" + data + '\'' +
                "]";
    }
}
