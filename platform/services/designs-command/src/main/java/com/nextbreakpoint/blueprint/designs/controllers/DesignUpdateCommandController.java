package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.commands.DesignUpdateCommand;
import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.DesignUpdateRequested;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.common.vertx.KafkaEmitter;
import com.nextbreakpoint.blueprint.designs.Store;
import rx.Single;

import java.util.Objects;

public class DesignUpdateCommandController implements Controller<InputMessage, Void> {
    private final Mapper<InputMessage, DesignUpdateCommand> inputMapper;
    private final MessageMapper<DesignUpdateRequested, OutputMessage> outputMapper;
    private final KafkaEmitter emitter;
    private final Store store;

    public DesignUpdateCommandController(Store store, Mapper<InputMessage, DesignUpdateCommand> inputMapper, MessageMapper<DesignUpdateRequested, OutputMessage> outputMapper, KafkaEmitter emitter) {
        this.store = Objects.requireNonNull(store);
        this.inputMapper = Objects.requireNonNull(inputMapper);
        this.outputMapper = Objects.requireNonNull(outputMapper);
        this.emitter = Objects.requireNonNull(emitter);
    }

    @Override
    public Single<Void> onNext(InputMessage message) {
        return Single.just(message)
                .flatMap(this::onMessageReceived)
                .map(inputMapper::transform)
                .flatMap(this::onDesignUpdateRequested)
                .map(event -> outputMapper.transform(Tracing.from(message.getTrace()), event))
                .flatMap(emitter::onNext);
    }

    private Single<InputMessage> onMessageReceived(InputMessage message) {
        return store.appendMessage(message).map(result -> message);
    }

    private Single<DesignUpdateRequested> onDesignUpdateRequested(DesignUpdateCommand command) {
        return Single.just(createEvent(command));
    }

    private DesignUpdateRequested createEvent(DesignUpdateCommand command) {
        return DesignUpdateRequested.builder()
                .withDesignId(command.getDesignId())
                .withCommandId(command.getCommandId())
                .withUserId(command.getUserId())
                .withData(command.getData())
                .withLevels(command.getLevels())
                .build();
    }
}
