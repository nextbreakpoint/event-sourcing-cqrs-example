package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.commands.DesignDeleteCommand;
import com.nextbreakpoint.blueprint.common.core.Controller;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.MessageEmitter;
import com.nextbreakpoint.blueprint.common.core.MessageMapper;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.DesignDeleteRequested;
import com.nextbreakpoint.blueprint.designs.Store;
import rx.Single;

import java.util.Objects;

public class DesignDeleteCommandController implements Controller<InputMessage, Void> {
    private final Mapper<InputMessage, DesignDeleteCommand> inputMapper;
    private final MessageMapper<DesignDeleteRequested, OutputMessage> outputMapper;
    private final MessageEmitter emitter;
    private final Store store;

    public DesignDeleteCommandController(Store store, Mapper<InputMessage, DesignDeleteCommand> inputMapper, MessageMapper<DesignDeleteRequested, OutputMessage> outputMapper, MessageEmitter emitter) {
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
                .flatMap(this::onDesignDeleteRequested)
                .map(outputMapper::transform)
                .flatMap(emitter::send);
    }

    private Single<InputMessage> onMessageReceived(InputMessage message) {
        return store.appendMessage(message).map(result -> message);
    }

    private Single<DesignDeleteRequested> onDesignDeleteRequested(DesignDeleteCommand command) {
        return Single.just(createEvent(command));
    }

    private DesignDeleteRequested createEvent(DesignDeleteCommand command) {
        return DesignDeleteRequested.builder()
                .withDesignId(command.getDesignId())
                .withCommandId(command.getCommandId())
                .withUserId(command.getUserId())
                .build();
    }
}
