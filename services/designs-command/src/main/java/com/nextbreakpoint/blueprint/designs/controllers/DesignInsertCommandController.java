package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.commands.avro.DesignInsertCommand;
import com.nextbreakpoint.blueprint.common.core.Controller;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.MessageEmitter;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.avro.DesignInsertRequested;
import com.nextbreakpoint.blueprint.common.vertx.MessageFactory;
import com.nextbreakpoint.blueprint.designs.Store;
import rx.Single;

import java.util.Objects;

public class DesignInsertCommandController implements Controller<InputMessage<DesignInsertCommand>, Void> {
    private final Store store;
    private final String messageSource;
    private final MessageEmitter<DesignInsertRequested> emitter;

    public DesignInsertCommandController(Store store, String messageSource, MessageEmitter<DesignInsertRequested> emitter) {
        this.store = Objects.requireNonNull(store);
        this.messageSource = Objects.requireNonNull(messageSource);
        this.emitter = Objects.requireNonNull(emitter);
    }

    @Override
    public Single<Void> onNext(InputMessage<DesignInsertCommand> message) {
        return Single.just(message)
                .flatMap(this::onMessageReceived)
                .flatMap(emitter::send);
    }

    private Single<OutputMessage<DesignInsertRequested>> onMessageReceived(InputMessage<DesignInsertCommand> message) {
        return store.appendMessage(message)
                .map(ignore -> message.getValue())
                .map(payload -> createEvent(payload.getData()))
                .map(this::createMessage);
    }

    private DesignInsertRequested createEvent(DesignInsertCommand command) {
        return DesignInsertRequested.newBuilder()
                .setDesignId(command.getDesignId())
                .setCommandId(command.getCommandId())
                .setUserId(command.getUserId())
                .setData(command.getData())
                .build();
    }

    private OutputMessage<DesignInsertRequested> createMessage(DesignInsertRequested event) {
        return MessageFactory.<DesignInsertRequested>of(messageSource)
                .createOutputMessage(event.getDesignId().toString(), event);
    }
}
