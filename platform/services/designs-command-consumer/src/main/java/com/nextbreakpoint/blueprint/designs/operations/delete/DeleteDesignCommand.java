package com.nextbreakpoint.blueprint.designs.operations.delete;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextbreakpoint.blueprint.designs.model.Command;

import java.util.UUID;

public class DeleteDesignCommand extends Command {
    @JsonCreator
    public DeleteDesignCommand(@JsonProperty("uuid") UUID uuid,
                               @JsonProperty("timestamp") Long timestamp) {
        super(uuid, timestamp);
    }
}
