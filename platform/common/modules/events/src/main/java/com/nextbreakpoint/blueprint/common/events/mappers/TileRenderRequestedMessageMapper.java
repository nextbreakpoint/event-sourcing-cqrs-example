package com.nextbreakpoint.blueprint.common.events.mappers;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import com.nextbreakpoint.blueprint.common.events.TileRenderRequested;
import io.vertx.core.json.Json;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

public class TileRenderRequestedMessageMapper implements Mapper<TileRenderRequested, Message> {
    private final String messageSource;
    private Function<TileRenderRequested, String> keyMapper;

    public TileRenderRequestedMessageMapper(String messageSource, Function<TileRenderRequested, String> keyMapper) {
        this.messageSource = Objects.requireNonNull(messageSource);
        this.keyMapper = Objects.requireNonNull(keyMapper);
    }

    public TileRenderRequestedMessageMapper(String messageSource) {
        this(messageSource, event -> event.getUuid().toString());
    }

    @Override
    public Message transform(TileRenderRequested event) {
        return new Message(
                UUID.randomUUID().toString(),
                MessageType.TILE_RENDER_REQUESTED,
                Json.encode(event),
                messageSource,
                keyMapper.apply(event),
                System.currentTimeMillis()
        );
    }
}
