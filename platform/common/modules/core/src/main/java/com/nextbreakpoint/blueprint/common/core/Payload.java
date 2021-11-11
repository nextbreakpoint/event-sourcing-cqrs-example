package com.nextbreakpoint.blueprint.common.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.UUID;

public class Payload {
    private UUID uuid;
    private String type;
    private String data;
    private String source;

    @JsonCreator
    public Payload(
        @JsonProperty("uuid") UUID uuid,
        @JsonProperty("type") String type,
        @JsonProperty("data") String data,
        @JsonProperty("source") String source
    ) {
        this.uuid = Objects.requireNonNull(uuid);
        this.type = Objects.requireNonNull(type);
        this.data = Objects.requireNonNull(data);
        this.source = Objects.requireNonNull(source);
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getType() {
        return type;
    }

    public String getData() {
        return data;
    }

    public String getSource() {
        return source;
    }

    @Override
    public String toString() {
        return "[" +
                "uuid=" + uuid +
                ", type='" + type + '\'' +
                ", data='" + data + '\'' +
                ", source='" + source + '\'' +
                "]";
    }
}
