package com.nextbreakpoint.blueprint.designs.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class VersionCreated {
    private final String checksum;
    private final String data;

    @JsonCreator
    public VersionCreated(
        @JsonProperty("checksum") String checksum,
        @JsonProperty("data") String data
    ) {
        this.checksum = Objects.requireNonNull(checksum);
        this.data = Objects.requireNonNull(data);
    }

    public String getChecksum() {
        return checksum;
    }

    public String getData() {
        return data;
    }
}
