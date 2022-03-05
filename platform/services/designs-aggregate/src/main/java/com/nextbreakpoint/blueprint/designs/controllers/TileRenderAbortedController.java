package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.Tombstone;
import com.nextbreakpoint.blueprint.common.events.TileRenderAborted;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.common.vertx.TombstoneEmitter;
import com.nextbreakpoint.blueprint.designs.common.Render;
import rx.Single;

import java.util.Objects;

public class TileRenderAbortedController implements Controller<InputMessage, Void> {
    private final Mapper<InputMessage, TileRenderAborted> inputMapper;
    private final TombstoneEmitter emitter;

    public TileRenderAbortedController(Mapper<InputMessage, TileRenderAborted> inputMapper, TombstoneEmitter emitter) {
        this.inputMapper = Objects.requireNonNull(inputMapper);
        this.emitter = Objects.requireNonNull(emitter);
    }

    @Override
    public Single<Void> onNext(InputMessage message) {
        return Single.just(message)
                .map(inputMapper::transform)
                .map(this::onTileRenderAborted)
                .flatMap(emitter::onNext);
    }

    private Tombstone onTileRenderAborted(TileRenderAborted event) {
        return new Tombstone(Render.createRenderKey(event));
    }
}
