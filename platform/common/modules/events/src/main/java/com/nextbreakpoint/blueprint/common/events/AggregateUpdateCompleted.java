package com.nextbreakpoint.blueprint.common.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.UUID;

public class AggregateUpdateCompleted {
    private final UUID uuid;
    private final UUID evid;
    private final String data;
    private final String checksum;

    @JsonCreator
    public AggregateUpdateCompleted(
        @JsonProperty("uuid") UUID uuid,
        @JsonProperty("evid") UUID evid,
        @JsonProperty("data") String data,
        @JsonProperty("checksum") String checksum
    ) {
        this.uuid = Objects.requireNonNull(uuid);
        this.evid = Objects.requireNonNull(evid);
        this.data = Objects.requireNonNull(data);
        this.checksum = Objects.requireNonNull(checksum);
    }

    public UUID getUuid() {
        return uuid;
    }

    public UUID getEvid() {
        return evid;
    }

    public String getData() {
        return data;
    }

    public String getChecksum() {
        return checksum;
    }
}
