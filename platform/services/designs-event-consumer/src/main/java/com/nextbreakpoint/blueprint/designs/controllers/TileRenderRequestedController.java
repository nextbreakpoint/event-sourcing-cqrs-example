package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.events.TileRenderRequested;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.common.vertx.KafkaEmitter;
import rx.Single;

import java.util.Objects;

public class TileRenderRequestedController implements Controller<Message, Void> {
    private final Mapper<Message, TileRenderRequested> inputMapper;
    private final Mapper<TileRenderRequested, Message> outputMapper;
    private final KafkaEmitter emitter;

    public TileRenderRequestedController(Mapper<Message, TileRenderRequested> inputMapper, Mapper<TileRenderRequested, Message> outputMapper, KafkaEmitter emitter) {
        this.inputMapper = Objects.requireNonNull(inputMapper);
        this.outputMapper = Objects.requireNonNull(outputMapper);
        this.emitter = Objects.requireNonNull(emitter);
    }

    @Override
    public Single<Void> onNext(Message message) {
        return Single.just(message)
                .map(inputMapper::transform)
                .map(outputMapper::transform)
                .flatMap(emitter::onNext);
    }
}
