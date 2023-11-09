package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.Controller;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.MessageEmitter;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentDeleteCompleted;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentDeleteRequested;
import com.nextbreakpoint.blueprint.designs.Store;
import com.nextbreakpoint.blueprint.designs.persistence.dto.DeleteDesignRequest;
import rx.Observable;
import rx.Single;

import java.util.Objects;

public class DesignDocumentDeleteRequestedController implements Controller<InputMessage, Void> {
    private final Store store;
    private final Mapper<InputMessage, DesignDocumentDeleteRequested> inputMapper;
    private final Mapper<DesignDocumentDeleteCompleted, OutputMessage> outputMapper;
    private final MessageEmitter emitter;

    public DesignDocumentDeleteRequestedController(Store store, Mapper<InputMessage, DesignDocumentDeleteRequested> inputMapper, Mapper<DesignDocumentDeleteCompleted, OutputMessage> outputMapper, MessageEmitter emitter) {
        this.store = Objects.requireNonNull(store);
        this.inputMapper = Objects.requireNonNull(inputMapper);
        this.outputMapper = Objects.requireNonNull(outputMapper);
        this.emitter = Objects.requireNonNull(emitter);
    }

    @Override
    public Single<Void> onNext(InputMessage message) {
        return Single.just(message)
                .map(inputMapper::transform)
                .flatMapObservable(this::onDesignDocumentDeleteRequested)
                .map(outputMapper::transform)
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
}
