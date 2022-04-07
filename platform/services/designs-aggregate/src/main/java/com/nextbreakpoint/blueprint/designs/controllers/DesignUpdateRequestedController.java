package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.DesignAggregateUpdateRequested;
import com.nextbreakpoint.blueprint.common.events.DesignUpdateRequested;
import com.nextbreakpoint.blueprint.designs.aggregate.DesignAggregate;
import rx.Single;

import java.util.Objects;

public class DesignUpdateRequestedController implements Controller<InputMessage, Void> {
    private final Mapper<InputMessage, DesignUpdateRequested> inputMapper;
    private final MessageMapper<DesignAggregateUpdateRequested, OutputMessage> outputMapper;
    private final MessageEmitter emitter;
    private final DesignAggregate aggregate;

    public DesignUpdateRequestedController(DesignAggregate aggregate, Mapper<InputMessage, DesignUpdateRequested> inputMapper, MessageMapper<DesignAggregateUpdateRequested, OutputMessage> outputMapper, MessageEmitter emitter) {
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
                .flatMap(event -> onDesignUpdateRequested(event, message.getToken()))
                .map(outputMapper::transform)
                .flatMap(emitter::send);
    }

    private Single<InputMessage> onMessageReceived(InputMessage message) {
        return aggregate.appendMessage(message).map(result -> message);
    }

    private Single<DesignAggregateUpdateRequested> onDesignUpdateRequested(DesignUpdateRequested event, String revision) {
        return Single.just(createEvent(event, revision));
    }

    private DesignAggregateUpdateRequested createEvent(DesignUpdateRequested event, String revision) {
        return DesignAggregateUpdateRequested.builder()
                .withDesignId(event.getDesignId())
                .withCommandId(event.getCommandId())
                .withRevision(revision)
                .build();
    }
}
