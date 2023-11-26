package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.commands.avro.DesignDeleteCommand;
import com.nextbreakpoint.blueprint.common.core.Controller;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.MessageEmitter;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDeleteRequested;
import com.nextbreakpoint.blueprint.common.vertx.MessageFactory;
import com.nextbreakpoint.blueprint.designs.Store;
import rx.Single;

import java.util.Objects;

public class DesignDeleteCommandController implements Controller<InputMessage<DesignDeleteCommand>, Void> {
    private final Store store;
    private final String messageSource;
    private final MessageEmitter<DesignDeleteRequested> emitter;

    public DesignDeleteCommandController(Store store, String messageSource, MessageEmitter<DesignDeleteRequested> emitter) {
        this.store = Objects.requireNonNull(store);
        this.messageSource = Objects.requireNonNull(messageSource);
        this.emitter = Objects.requireNonNull(emitter);
    }

    @Override
    public Single<Void> onNext(InputMessage<DesignDeleteCommand> message) {
        return Single.just(message)
                .flatMap(this::onMessageReceived)
                .flatMap(emitter::send);
    }

    private Single<OutputMessage<DesignDeleteRequested>> onMessageReceived(InputMessage<DesignDeleteCommand> message) {
        return store.appendMessage(message)
                .map(ignore -> message.getValue())
                .map(payload -> createEvent(payload.getData()))
                .map(this::createMessage);
    }

    private DesignDeleteRequested createEvent(DesignDeleteCommand command) {
        return DesignDeleteRequested.newBuilder()
                .setDesignId(command.getDesignId())
                .setCommandId(command.getCommandId())
                .setUserId(command.getUserId())
                .build();
    }

    private OutputMessage<DesignDeleteRequested> createMessage(DesignDeleteRequested event) {
        return MessageFactory.<DesignDeleteRequested>of(messageSource)
                .createOutputMessage(event.getDesignId().toString(), event);
    }
}
