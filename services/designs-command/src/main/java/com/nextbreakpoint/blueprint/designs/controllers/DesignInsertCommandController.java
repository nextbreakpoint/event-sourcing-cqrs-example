package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.commands.DesignInsertCommand;
import com.nextbreakpoint.blueprint.common.core.Controller;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.MessageEmitter;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.DesignInsertRequested;
import com.nextbreakpoint.blueprint.designs.Store;
import rx.Single;

import java.util.Objects;

public class DesignInsertCommandController implements Controller<InputMessage, Void> {
    private final Mapper<InputMessage, DesignInsertCommand> inputMapper;
    private final Mapper<DesignInsertRequested, OutputMessage> outputMapper;
    private final MessageEmitter emitter;
    private final Store store;

    public DesignInsertCommandController(Store store, Mapper<InputMessage, DesignInsertCommand> inputMapper, Mapper<DesignInsertRequested, OutputMessage> outputMapper, MessageEmitter emitter) {
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
                .map(outputMapper::transform)
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
                .build();
    }
}
