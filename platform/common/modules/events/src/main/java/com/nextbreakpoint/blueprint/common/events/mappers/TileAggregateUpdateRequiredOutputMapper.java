package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.core.Payload;
import com.nextbreakpoint.blueprint.common.events.TileAggregateUpdateRequired;

import java.util.Objects;
import java.util.UUID;

public class TileAggregateUpdateRequiredOutputMapper implements Mapper<TileAggregateUpdateRequired, OutputMessage> {
    private final String messageSource;

    public TileAggregateUpdateRequiredOutputMapper(String messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public OutputMessage transform(TileAggregateUpdateRequired event) {
        return new OutputMessage(
                event.getUuid().toString(),
                new Payload(
                        UUID.randomUUID(),
                        TileAggregateUpdateRequired.TYPE,
                        Json.encodeValue(event),
                        messageSource
                )
        );
    }
}
