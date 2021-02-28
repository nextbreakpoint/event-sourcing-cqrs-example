package com.nextbreakpoint.blueprint.designs.model;

import com.nextbreakpoint.blueprint.common.core.event.DesignChanged;

import java.util.Objects;

public class CommandOutput {
    private final DesignChanged event;

    public CommandOutput(DesignChanged event) {
        this.event = Objects.requireNonNull(event);
    }

    public DesignChanged getEvent() {
        return event;
    }
}
