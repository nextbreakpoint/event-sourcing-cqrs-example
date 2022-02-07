package com.nextbreakpoint.blueprint.designs.controllers;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.TileAggregateUpdateRequested;
import com.nextbreakpoint.blueprint.common.events.TileAggregateUpdateRequired;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.common.vertx.KafkaEmitter;
import rx.Single;

import java.util.Objects;

public class TileAggregateUpdateRequiredController implements Controller<InputMessage, Void> {
    private final Mapper<InputMessage, TileAggregateUpdateRequired> inputMapper;
    private final Mapper<TileAggregateUpdateRequested, OutputMessage> outputMapper;
    private final KafkaEmitter emitter;

    public TileAggregateUpdateRequiredController(Mapper<InputMessage, TileAggregateUpdateRequired> inputMapper, Mapper<TileAggregateUpdateRequested, OutputMessage> outputMapper, KafkaEmitter emitter) {
        this.inputMapper =  Objects.requireNonNull(inputMapper);
        this.outputMapper = Objects.requireNonNull(outputMapper);
        this.emitter = Objects.requireNonNull(emitter);
    }

    @Override
    public Single<Void> onNext(InputMessage message) {
        return Single.just(message)
                .map(inputMapper::transform)
                .flatMap(this::onAggregateUpdateRequired)
                .map(outputMapper::transform)
                .flatMap(emitter::onNext);
    }

    private Single<TileAggregateUpdateRequested> onAggregateUpdateRequired(TileAggregateUpdateRequired event) {
        return Single.just(createEvent(event));
    }

    private TileAggregateUpdateRequested createEvent(TileAggregateUpdateRequired event) {
        return TileAggregateUpdateRequested.builder()
                .withEvid(Uuids.timeBased())
                .withUuid(event.getUuid())
                .withEsid(event.getEsid())
                .build();
    }
}
