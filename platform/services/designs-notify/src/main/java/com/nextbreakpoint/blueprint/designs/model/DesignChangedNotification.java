package com.nextbreakpoint.blueprint.designs.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;

import java.util.Objects;

@Data
@Builder(access = AccessLevel.PUBLIC, setterPrefix = "with")
public class DesignChangedNotification {
    private final String key;
    private final String revision;

    @JsonCreator
    public DesignChangedNotification(
        @JsonProperty("key") String key,
        @JsonProperty("revision") String revision
    ) {
        this.key = Objects.requireNonNull(key);
        this.revision = Objects.requireNonNull(revision);
    }

    public String getKey() {
        return key;
    }

    public String getRevision() {
        return revision;
    }
}
