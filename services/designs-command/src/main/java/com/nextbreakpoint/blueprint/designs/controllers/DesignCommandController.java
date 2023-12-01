package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.commands.avro.DesignDeleteCommand;
import com.nextbreakpoint.blueprint.common.commands.avro.DesignInsertCommand;
import com.nextbreakpoint.blueprint.common.commands.avro.DesignUpdateCommand;
import com.nextbreakpoint.blueprint.common.core.Controller;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.MessageEmitter;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDeleteRequested;
import com.nextbreakpoint.blueprint.common.events.avro.DesignInsertRequested;
import com.nextbreakpoint.blueprint.common.events.avro.DesignUpdateRequested;
import com.nextbreakpoint.blueprint.common.vertx.MessageFactory;
import com.nextbreakpoint.blueprint.designs.Store;
import org.apache.avro.specific.SpecificRecord;
import rx.Single;

import java.util.Objects;

public abstract class DesignCommandController<C extends SpecificRecord, E extends SpecificRecord> implements Controller<InputMessage<C>, Void> {
    private final Store store;
    private final String messageSource;
    private final MessageEmitter<E> emitter;

    public DesignCommandController(String messageSource, Store store, MessageEmitter<E> emitter) {
        this.store = Objects.requireNonNull(store);
        this.messageSource = Objects.requireNonNull(messageSource);
        this.emitter = Objects.requireNonNull(emitter);
    }

    @Override
    public Single<Void> onNext(InputMessage<C> message) {
        return Single.just(message)
                .flatMap(this::onMessageReceived)
                .flatMap(emitter::send);
    }

    private Single<OutputMessage<E>> onMessageReceived(InputMessage<C> message) {
        return store.appendMessage(message)
                .map(ignore -> message.getValue())
                .map(payload -> createEvent(payload.getData()))
                .map(this::createMessage);
    }

    private OutputMessage<E> createMessage(E event) {
        return MessageFactory.<E>of(messageSource)
                .createOutputMessage(extractKey(event), event);
    }

    protected abstract E createEvent(C command);

    protected abstract String extractKey(E event);

    public static class DesignInsertCommandController extends DesignCommandController<DesignInsertCommand, DesignInsertRequested> {
        public DesignInsertCommandController(String messageSource, Store store, MessageEmitter<DesignInsertRequested> emitter) {
            super(messageSource, store, emitter);
        }

        @Override
        protected DesignInsertRequested createEvent(DesignInsertCommand command) {
            return DesignInsertRequested.newBuilder()
                    .setDesignId(command.getDesignId())
                    .setCommandId(command.getCommandId())
                    .setUserId(command.getUserId())
                    .setData(command.getData())
                    .build();
        }

        @Override
        protected String extractKey(DesignInsertRequested event) {
            return event.getDesignId().toString();
        }
    }

    public static class DesignUpdateCommandController extends DesignCommandController<DesignUpdateCommand, DesignUpdateRequested> {
        public DesignUpdateCommandController(String messageSource, Store store, MessageEmitter<DesignUpdateRequested> emitter) {
            super(messageSource, store, emitter);
        }

        @Override
        protected DesignUpdateRequested createEvent(DesignUpdateCommand command) {
            return DesignUpdateRequested.newBuilder()
                    .setDesignId(command.getDesignId())
                    .setCommandId(command.getCommandId())
                    .setUserId(command.getUserId())
                    .setData(command.getData())
                    .setPublished(command.getPublished())
                    .build();
        }

        @Override
        protected String extractKey(DesignUpdateRequested event) {
            return event.getDesignId().toString();
        }
    }

    public static class DesignDeleteCommandController extends DesignCommandController<DesignDeleteCommand, DesignDeleteRequested> {
        public DesignDeleteCommandController(String messageSource, Store store, MessageEmitter<DesignDeleteRequested> emitter) {
            super(messageSource, store, emitter);
        }

        @Override
        protected DesignDeleteRequested createEvent(DesignDeleteCommand command) {
            return DesignDeleteRequested.newBuilder()
                .setDesignId(command.getDesignId())
                .setCommandId(command.getCommandId())
                .setUserId(command.getUserId())
                .build();
        }

        @Override
        protected String extractKey(DesignDeleteRequested event) {
            return event.getDesignId().toString();
        }
    }
}
