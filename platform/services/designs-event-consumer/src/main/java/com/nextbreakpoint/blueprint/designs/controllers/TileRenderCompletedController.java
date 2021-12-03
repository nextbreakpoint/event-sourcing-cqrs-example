package com.nextbreakpoint.blueprint.designs.controllers;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.TileAggregateUpdateRequired;
import com.nextbreakpoint.blueprint.common.events.TileRenderCompleted;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.common.vertx.KafkaEmitter;
import com.nextbreakpoint.blueprint.designs.aggregate.DesignAggregateManager;
import rx.Single;

import java.util.Objects;

public class TileRenderCompletedController implements Controller<InputMessage, Void> {
    private final Mapper<InputMessage, TileRenderCompleted> inputMapper;
    private final Mapper<TileAggregateUpdateRequired, OutputMessage> outputMapper;
    private final KafkaEmitter emitter;
    private final DesignAggregateManager aggregateManager;

    public TileRenderCompletedController(DesignAggregateManager aggregateManager, Mapper<InputMessage, TileRenderCompleted> inputMapper, Mapper<TileAggregateUpdateRequired, OutputMessage> outputMapper, KafkaEmitter emitter) {
        this.aggregateManager = Objects.requireNonNull(aggregateManager);
        this.inputMapper = Objects.requireNonNull(inputMapper);
        this.outputMapper = Objects.requireNonNull(outputMapper);
        this.emitter = Objects.requireNonNull(emitter);
    }

    @Override
    public Single<Void> onNext(InputMessage message) {
        return Single.just(message).flatMap(this::onMessageReceived);
    }

    private Single<Void> onMessageReceived(InputMessage message) {
        return aggregateManager.appendMessage(message)
                .map(result -> inputMapper.transform(message))
                .map(event -> createEvent(message, event))
                .map(outputMapper::transform)
                .flatMap(emitter::onNext);
    }

    private TileAggregateUpdateRequired createEvent(InputMessage message, TileRenderCompleted event) {
        return TileAggregateUpdateRequired.builder()
                .withEvid(Uuids.timeBased())
                .withUuid(event.getUuid())
                .withEsid(message.getOffset())
                .build();
    }
}
