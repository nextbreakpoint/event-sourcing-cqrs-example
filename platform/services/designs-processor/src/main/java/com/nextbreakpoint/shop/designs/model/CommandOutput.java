package com.nextbreakpoint.shop.designs.model;

import com.nextbreakpoint.shop.common.model.events.DesignChangedEvent;

import java.util.Objects;

public class CommandOutput {
    private final DesignChangedEvent event;

    public CommandOutput(DesignChangedEvent event) {
        this.event = Objects.requireNonNull(event);
    }

    public DesignChangedEvent getEvent() {
        return event;
    }
}
