package com.nextbreakpoint.blueprint.common.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.UUID;

public class DesignAggregateUpdateRequested {
    private final UUID evid;
    private final UUID uuid;
    private final UUID esid;

    @JsonCreator
    public DesignAggregateUpdateRequested(
        @JsonProperty("evid") UUID evid,
        @JsonProperty("uuid") UUID uuid,
        @JsonProperty("esid") UUID esid
    ) {
        this.evid = Objects.requireNonNull(evid);
        this.uuid = Objects.requireNonNull(uuid);
        this.esid = Objects.requireNonNull(esid);
    }

    public UUID getEvid() {
        return evid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public UUID getEsid() {
        return esid;
    }
}
