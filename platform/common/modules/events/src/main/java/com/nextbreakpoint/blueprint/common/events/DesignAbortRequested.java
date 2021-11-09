package com.nextbreakpoint.blueprint.common.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.UUID;

public class DesignAbortRequested {
    private final UUID evid;
    private final UUID uuid;
    private final String checksum;

    @JsonCreator
    public DesignAbortRequested(
        @JsonProperty("evid") UUID evid,
        @JsonProperty("uuid") UUID uuid,
        @JsonProperty("checksum") String checksum
    ) {
        this.evid = Objects.requireNonNull(evid);
        this.uuid = Objects.requireNonNull(uuid);
        this.checksum = Objects.requireNonNull(checksum);
    }

    public UUID getEvid() {
        return evid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getChecksum() {
        return checksum;
    }
}
