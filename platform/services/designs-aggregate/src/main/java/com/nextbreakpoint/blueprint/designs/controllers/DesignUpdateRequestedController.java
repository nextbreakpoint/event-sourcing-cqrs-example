package com.nextbreakpoint.blueprint.designs.controllers;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.DesignAggregateUpdateRequested;
import com.nextbreakpoint.blueprint.common.events.DesignUpdateRequested;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.common.vertx.KafkaEmitter;
import com.nextbreakpoint.blueprint.designs.aggregate.DesignAggregate;
import rx.Single;

import java.util.Objects;

public class DesignUpdateRequestedController implements Controller<InputMessage, Void> {
    private final Mapper<InputMessage, DesignUpdateRequested> inputMapper;
    private final MessageMapper<DesignAggregateUpdateRequested, OutputMessage> outputMapper;
    private final KafkaEmitter emitter;
    private final DesignAggregate aggregate;

    public DesignUpdateRequestedController(DesignAggregate aggregate, Mapper<InputMessage, DesignUpdateRequested> inputMapper, MessageMapper<DesignAggregateUpdateRequested, OutputMessage> outputMapper, KafkaEmitter emitter) {
        this.aggregate = Objects.requireNonNull(aggregate);
        this.inputMapper = Objects.requireNonNull(inputMapper);
        this.outputMapper = Objects.requireNonNull(outputMapper);
        this.emitter = Objects.requireNonNull(emitter);
    }

    @Override
    public Single<Void> onNext(InputMessage message) {
        return Single.just(message)
                .flatMap(this::onMessageReceived)
                .map(inputMapper::transform)
                .flatMap(event -> onDesignUpdateRequested(event, message.getOffset()))
                .map(event -> outputMapper.transform(Tracing.from(message.getTrace()), event))
                .flatMap(emitter::onNext);
    }

    private Single<InputMessage> onMessageReceived(InputMessage message) {
        return aggregate.appendMessage(message).map(result -> message);
    }

    private Single<DesignAggregateUpdateRequested> onDesignUpdateRequested(DesignUpdateRequested event, Long offset) {
        return Single.just(createEvent(event, offset));
    }

    private DesignAggregateUpdateRequested createEvent(DesignUpdateRequested event, Long offset) {
        return DesignAggregateUpdateRequested.builder()
                .withEventId(Uuids.timeBased())
                .withDesignId(event.getDesignId())
                .withRevision(offset)
                .build();
    }
}
