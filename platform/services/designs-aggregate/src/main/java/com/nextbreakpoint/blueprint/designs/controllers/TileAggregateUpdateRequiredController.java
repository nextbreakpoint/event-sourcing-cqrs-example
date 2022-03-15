package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.TileAggregateUpdateRequested;
import com.nextbreakpoint.blueprint.common.events.TileAggregateUpdateRequired;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.common.vertx.MessageEmitter;
import rx.Single;

import java.util.Objects;

public class TileAggregateUpdateRequiredController implements Controller<InputMessage, Void> {
    private final Mapper<InputMessage, TileAggregateUpdateRequired> inputMapper;
    private final MessageMapper<TileAggregateUpdateRequested, OutputMessage> outputMapper;
    private final MessageEmitter emitter;

    public TileAggregateUpdateRequiredController(Mapper<InputMessage, TileAggregateUpdateRequired> inputMapper, MessageMapper<TileAggregateUpdateRequested, OutputMessage> outputMapper, MessageEmitter emitter) {
        this.inputMapper =  Objects.requireNonNull(inputMapper);
        this.outputMapper = Objects.requireNonNull(outputMapper);
        this.emitter = Objects.requireNonNull(emitter);
    }

    @Override
    public Single<Void> onNext(InputMessage message) {
        return Single.just(message)
                .map(inputMapper::transform)
                .flatMap(this::onAggregateUpdateRequired)
                .map(event -> outputMapper.transform(event, message.getTrace()))
                .flatMap(emitter::send);
    }

    private Single<TileAggregateUpdateRequested> onAggregateUpdateRequired(TileAggregateUpdateRequired event) {
        return Single.just(createEvent(event));
    }

    private TileAggregateUpdateRequested createEvent(TileAggregateUpdateRequired event) {
        return TileAggregateUpdateRequested.builder()
                .withDesignId(event.getDesignId())
                .withRevision(event.getRevision())
                .build();
    }
}
