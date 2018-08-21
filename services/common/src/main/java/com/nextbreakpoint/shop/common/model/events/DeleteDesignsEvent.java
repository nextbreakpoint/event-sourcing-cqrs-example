package com.nextbreakpoint.shop.common.model.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class DeleteDesignsEvent {
    private final Long timestamp;

    @JsonCreator
    public DeleteDesignsEvent(@JsonProperty("timestamp") Long timestamp) {
        this.timestamp = Objects.requireNonNull(timestamp);
    }

    public Long getTimestamp() {
        return timestamp;
    }
}