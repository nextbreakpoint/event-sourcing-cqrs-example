package com.nextbreakpoint.blueprint.common.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.UUID;

public class DesignAggregateUpdateCompleted {
    private final UUID evid;
    private final UUID uuid;
    private final UUID esid;
    private final String data;
    private final String checksum;
    private final int levels;
    private final String status;

    @JsonCreator
    public DesignAggregateUpdateCompleted(
        @JsonProperty("evid") UUID evid,
        @JsonProperty("uuid") UUID uuid,
        @JsonProperty("esid") UUID esid,
        @JsonProperty("data") String data,
        @JsonProperty("checksum") String checksum,
        @JsonProperty("levels") int levels,
        @JsonProperty("status") String status
    ) {
        this.evid = Objects.requireNonNull(evid);
        this.uuid = Objects.requireNonNull(uuid);
        this.esid = Objects.requireNonNull(esid);
        this.data = Objects.requireNonNull(data);
        this.checksum = Objects.requireNonNull(checksum);
        this.levels = levels;
        this.status = status;
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

    public String getData() {
        return data;
    }

    public String getChecksum() {
        return checksum;
    }

    public int getLevels() {
        return levels;
    }

    public String getStatus() {
        return status;
    }
}
