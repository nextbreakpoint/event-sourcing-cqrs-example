package com.nextbreakpoint.shop.designs.model;

import com.nextbreakpoint.shop.common.model.events.DesignChangedEvent;

import java.util.Objects;
import java.util.UUID;

public class CommandResult {
    private final UUID uuid;
    private final DesignChangedEvent event;

    public CommandResult(UUID uuid, DesignChangedEvent event) {
        this.uuid = Objects.requireNonNull(uuid);
        this.event = Objects.requireNonNull(event);
    }

    public UUID getUuid() {
        return uuid;
    }

    public DesignChangedEvent getEvent() {
        return event;
    }
}
