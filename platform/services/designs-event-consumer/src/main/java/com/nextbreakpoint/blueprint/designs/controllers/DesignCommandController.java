package com.nextbreakpoint.blueprint.designs.controllers;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.DesignAggregateUpdateRequested;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.common.vertx.KafkaEmitter;
import com.nextbreakpoint.blueprint.designs.aggregate.DesignAggregateManager;
import rx.Single;

import java.util.Objects;
import java.util.UUID;

public class DesignCommandController implements Controller<InputMessage, Void> {
    private final Mapper<DesignAggregateUpdateRequested, OutputMessage> outputMapper;
    private final KafkaEmitter emitter;
    private final DesignAggregateManager aggregateManager;

    public DesignCommandController(DesignAggregateManager aggregateManager, Mapper<DesignAggregateUpdateRequested, OutputMessage> outputMapper, KafkaEmitter emitter) {
        this.aggregateManager = Objects.requireNonNull(aggregateManager);
        this.outputMapper = Objects.requireNonNull(outputMapper);
        this.emitter = Objects.requireNonNull(emitter);
    }

    @Override
    public Single<Void> onNext(InputMessage message) {
        return Single.just(message)
                .flatMap(this::onMessageReceived)
                .map(outputMapper::transform)
                .flatMap(emitter::onNext);
    }

    private Single<DesignAggregateUpdateRequested> onMessageReceived(InputMessage message) {
        return aggregateManager.appendMessage(message).map(result -> createEvent(message));
    }

    private DesignAggregateUpdateRequested createEvent(InputMessage message) {
        return DesignAggregateUpdateRequested.builder()
                .withEvid(Uuids.timeBased())
                .withUuid(UUID.fromString(message.getKey()))
                .withEsid(message.getOffset())
                .build();
    }
}
