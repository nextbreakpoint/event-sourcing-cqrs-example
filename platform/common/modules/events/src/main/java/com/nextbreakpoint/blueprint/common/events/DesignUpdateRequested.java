package com.nextbreakpoint.blueprint.common.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;

import java.util.Objects;
import java.util.UUID;

@Data
@Builder(access = AccessLevel.PUBLIC, setterPrefix = "with")
public class DesignUpdateRequested {
    private final UUID evid;
    private final UUID uuid;
    private final String data;
    private final int levels;

    @JsonCreator
    public DesignUpdateRequested(
        @JsonProperty("evid") UUID evid,
        @JsonProperty("uuid") UUID uuid,
        @JsonProperty("data") String data,
        @JsonProperty("levels") int levels
    ) {
        this.evid = Objects.requireNonNull(evid);
        this.uuid = Objects.requireNonNull(uuid);
        this.data = Objects.requireNonNull(data);
        this.levels = levels;
    }

    public UUID getEvid() {
        return evid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getData() {
        return data;
    }

    public int getLevels() {
        return levels;
    }
}
