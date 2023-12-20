package com.nextbreakpoint.blueprint.designs.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;

import java.util.Objects;

@Data
@JsonPropertyOrder({
        "revision"
})
@Builder(access = AccessLevel.PUBLIC, setterPrefix = "with")
public class SessionUpdatedNotification {
    private final String revision;

    @JsonCreator
    public SessionUpdatedNotification(
        @JsonProperty("revision") String revision
    ) {
        this.revision = Objects.requireNonNull(revision);
    }
}
