package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.core.Tombstone;
import com.nextbreakpoint.blueprint.common.events.TileRenderAborted;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.common.vertx.TombstoneEmitter;
import com.nextbreakpoint.blueprint.designs.common.Bucket;
import rx.Single;

import java.util.Objects;

public class TileRenderAbortedController implements Controller<Message, Void> {
    private final Mapper<Message, TileRenderAborted> inputMapper;
    private final TombstoneEmitter emitter;

    public TileRenderAbortedController(Mapper<Message, TileRenderAborted> inputMapper, TombstoneEmitter emitter) {
        this.inputMapper = Objects.requireNonNull(inputMapper);
        this.emitter = Objects.requireNonNull(emitter);
    }

    @Override
    public Single<Void> onNext(Message message) {
        return Single.just(message)
                .map(inputMapper::transform)
                .map(this::onTileRenderAborted)
                .flatMap(emitter::onNext);
    }

    private Tombstone onTileRenderAborted(TileRenderAborted event) {
        return new Tombstone(Bucket.createBucketKey(event));
    }
}
