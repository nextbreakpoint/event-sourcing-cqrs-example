package com.nextbreakpoint.blueprint.common.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.UUID;

public class DesignAggregateUpdateCompleted {
    private final UUID uuid;
    private final Long timestamp;
    private final UUID evid;
    private final String data;
    private final String checksum;
    private final String status;

    @JsonCreator
    public DesignAggregateUpdateCompleted(
        @JsonProperty("uuid") UUID uuid,
        @JsonProperty("timestamp") Long timestamp,
        @JsonProperty("evid") UUID evid,
        @JsonProperty("data") String data,
        @JsonProperty("checksum") String checksum,
        @JsonProperty("status") String status
    ) {
        this.uuid = Objects.requireNonNull(uuid);
        this.timestamp = Objects.requireNonNull(timestamp);
        this.evid = Objects.requireNonNull(evid);
        this.data = Objects.requireNonNull(data);
        this.checksum = Objects.requireNonNull(checksum);
        this.status = status;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Long getTimestamp() {
        return timestamp;
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

    public String getStatus() {
        return status;
    }
}
