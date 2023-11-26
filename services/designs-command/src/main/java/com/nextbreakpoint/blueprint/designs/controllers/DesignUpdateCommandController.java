package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.commands.avro.DesignUpdateCommand;
import com.nextbreakpoint.blueprint.common.core.Controller;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.MessageEmitter;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.avro.DesignUpdateRequested;
import com.nextbreakpoint.blueprint.common.vertx.MessageFactory;
import com.nextbreakpoint.blueprint.designs.Store;
import rx.Single;

import java.util.Objects;

public class DesignUpdateCommandController implements Controller<InputMessage<DesignUpdateCommand>, Void> {
    private final Store store;
    private final String messageSource;
    private final MessageEmitter<DesignUpdateRequested> emitter;

    public DesignUpdateCommandController(Store store, String messageSource, MessageEmitter<DesignUpdateRequested> emitter) {
        this.store = Objects.requireNonNull(store);
        this.messageSource = Objects.requireNonNull(messageSource);
        this.emitter = Objects.requireNonNull(emitter);
    }

    @Override
    public Single<Void> onNext(InputMessage<DesignUpdateCommand> message) {
        return Single.just(message)
                .flatMap(this::onMessageReceived)
                .flatMap(emitter::send);
    }

    private Single<OutputMessage<DesignUpdateRequested>> onMessageReceived(InputMessage<DesignUpdateCommand> message) {
        return store.appendMessage(message)
                .map(ignore -> message.getValue())
                .map(payload -> createEvent(payload.getData()))
                .map(this::createMessage);
    }

    private DesignUpdateRequested createEvent(DesignUpdateCommand command) {
        return DesignUpdateRequested.newBuilder()
                .setDesignId(command.getDesignId())
                .setCommandId(command.getCommandId())
                .setUserId(command.getUserId())
                .setData(command.getData())
                .setPublished(command.getPublished())
                .build();
    }

    private OutputMessage<DesignUpdateRequested> createMessage(DesignUpdateRequested event) {
        return MessageFactory.<DesignUpdateRequested>of(messageSource)
                .createOutputMessage(event.getDesignId().toString(), event);
    }
}
