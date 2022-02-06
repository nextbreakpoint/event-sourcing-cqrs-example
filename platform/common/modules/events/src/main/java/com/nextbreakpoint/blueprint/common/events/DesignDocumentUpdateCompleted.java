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
public class DesignDocumentUpdateCompleted {
    public static final String TYPE = "design-document-update-completed-v1";

    private final UUID evid;
    private final UUID uuid;

    @JsonCreator
    public DesignDocumentUpdateCompleted(
        @JsonProperty("evid") UUID evid,
        @JsonProperty("uuid") UUID uuid
    ) {
        this.evid = Objects.requireNonNull(evid);
        this.uuid = Objects.requireNonNull(uuid);
    }

    public UUID getEvid() {
        return evid;
    }

    public UUID getUuid() {
        return uuid;
    }
}
