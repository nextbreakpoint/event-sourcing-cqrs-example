package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.TileRenderCancelled;
import com.nextbreakpoint.blueprint.common.events.TilesRendered;
import com.nextbreakpoint.blueprint.designs.aggregate.DesignAggregate;
import com.nextbreakpoint.blueprint.designs.model.Design;
import rx.Observable;
import rx.Single;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TileRenderCancelledController implements Controller<InputMessage, Void> {
    private final Mapper<InputMessage, TileRenderCancelled> inputMapper;
    private final MessageMapper<TileRenderCancelled, OutputMessage> outputMapper;
    private final MessageEmitter cancelEmitter;
    private final TombstoneEmitter renderEmitter;

    public TileRenderCancelledController(
            Mapper<InputMessage, TileRenderCancelled> inputMapper,
            MessageMapper<TileRenderCancelled, OutputMessage> outputMapper,
            MessageEmitter cancelEmitter,
            TombstoneEmitter renderEmitter
    ) {
        this.inputMapper = Objects.requireNonNull(inputMapper);
        this.outputMapper = Objects.requireNonNull(outputMapper);
        this.cancelEmitter = Objects.requireNonNull(cancelEmitter);
        this.renderEmitter = Objects.requireNonNull(renderEmitter);
    }

    @Override
    public Single<Void> onNext(InputMessage message) {
        return Single.just(null);
    }
}
