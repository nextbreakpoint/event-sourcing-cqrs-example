package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.MessageMapper;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.TileRenderRequested;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.common.vertx.MessageEmitter;
import rx.Single;

import java.util.Objects;

public class TileRenderRequestedController implements Controller<InputMessage, Void> {
    private final Mapper<InputMessage, TileRenderRequested> inputMapper;
    private final MessageMapper<TileRenderRequested, OutputMessage> outputMapper;
    private final MessageEmitter emitter;

    public TileRenderRequestedController(Mapper<InputMessage, TileRenderRequested> inputMapper, MessageMapper<TileRenderRequested, OutputMessage> outputMapper, MessageEmitter emitter) {
        this.inputMapper = Objects.requireNonNull(inputMapper);
        this.outputMapper = Objects.requireNonNull(outputMapper);
        this.emitter = Objects.requireNonNull(emitter);
    }

    @Override
    public Single<Void> onNext(InputMessage message) {
        return Single.just(message)
                .map(inputMapper::transform)
                .map(outputMapper::transform)
                .flatMap(emitter::send);
    }
}
