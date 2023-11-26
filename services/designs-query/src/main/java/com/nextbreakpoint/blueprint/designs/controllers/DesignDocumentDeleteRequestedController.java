package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.Controller;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.MessageEmitter;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDocumentDeleteCompleted;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDocumentDeleteRequested;
import com.nextbreakpoint.blueprint.common.vertx.MessageFactory;
import com.nextbreakpoint.blueprint.designs.Store;
import com.nextbreakpoint.blueprint.designs.persistence.dto.DeleteDesignRequest;
import rx.Observable;
import rx.Single;

import java.util.Objects;

public class DesignDocumentDeleteRequestedController implements Controller<InputMessage<DesignDocumentDeleteRequested>, Void> {
    private final String messageSource;
    private final Store store;
    private final MessageEmitter<DesignDocumentDeleteCompleted> emitter;

    public DesignDocumentDeleteRequestedController(String messageSource, Store store, MessageEmitter<DesignDocumentDeleteCompleted> emitter) {
        this.messageSource = Objects.requireNonNull(messageSource);
        this.store = Objects.requireNonNull(store);
        this.emitter = Objects.requireNonNull(emitter);
    }

    @Override
    public Single<Void> onNext(InputMessage<DesignDocumentDeleteRequested> message) {
        return Single.fromCallable(() -> message.getValue().getData())
                .flatMapObservable(this::onDesignDocumentDeleteRequested)
                .map(this::createMessage)
                .flatMapSingle(emitter::send)
                .ignoreElements()
                .toCompletable()
                .toSingleDefault("")
                .map(result -> null);
    }

    private Observable<DesignDocumentDeleteCompleted> onDesignDocumentDeleteRequested(DesignDocumentDeleteRequested event) {
        return store.deleteDesign(new DeleteDesignRequest(event.getDesignId(), false))
                .flatMap(result -> store.deleteDesign(new DeleteDesignRequest(event.getDesignId(), true)))
                .map(result -> new DesignDocumentDeleteCompleted(event.getDesignId(), event.getCommandId(), event.getRevision()))
                .toObservable();
    }

    private OutputMessage<DesignDocumentDeleteCompleted> createMessage(DesignDocumentDeleteCompleted event) {
        return MessageFactory.<DesignDocumentDeleteCompleted>of(messageSource)
                .createOutputMessage(event.getDesignId().toString(), event);
    }
}
