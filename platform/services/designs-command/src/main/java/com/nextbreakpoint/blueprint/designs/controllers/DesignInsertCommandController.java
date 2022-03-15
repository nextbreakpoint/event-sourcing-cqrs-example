package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.commands.DesignInsertCommand;
import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.DesignInsertRequested;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.common.vertx.MessageEmitter;
import com.nextbreakpoint.blueprint.designs.Store;
import rx.Single;

import java.util.Objects;

public class DesignInsertCommandController implements Controller<InputMessage, Void> {
    private final Mapper<InputMessage, DesignInsertCommand> inputMapper;
    private final MessageMapper<DesignInsertRequested, OutputMessage> outputMapper;
    private final MessageEmitter emitter;
    private final Store store;

    public DesignInsertCommandController(Store store, Mapper<InputMessage, DesignInsertCommand> inputMapper, MessageMapper<DesignInsertRequested, OutputMessage> outputMapper, MessageEmitter emitter) {
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
                .flatMap(this::onDesignInsertRequested)
                .map(event -> outputMapper.transform(event, message.getTrace()))
                .flatMap(emitter::send);
    }

    private Single<InputMessage> onMessageReceived(InputMessage message) {
        return store.appendMessage(message).map(result -> message);
    }

    private Single<DesignInsertRequested> onDesignInsertRequested(DesignInsertCommand command) {
        return Single.just(createEvent(command));
    }

    private DesignInsertRequested createEvent(DesignInsertCommand command) {
        return DesignInsertRequested.builder()
                .withDesignId(command.getDesignId())
                .withCommandId(command.getCommandId())
                .withUserId(command.getUserId())
                .withData(command.getData())
                .withLevels(command.getLevels())
                .build();
    }
}
