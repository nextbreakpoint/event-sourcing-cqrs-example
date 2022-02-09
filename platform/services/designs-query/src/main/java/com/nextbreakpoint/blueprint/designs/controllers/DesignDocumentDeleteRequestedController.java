package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentDeleteCompleted;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentDeleteRequested;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.common.vertx.KafkaEmitter;
import com.nextbreakpoint.blueprint.designs.Store;
import com.nextbreakpoint.blueprint.designs.persistence.DeleteDesignRequest;
import rx.Observable;
import rx.Single;

import java.util.Objects;

public class DesignDocumentDeleteRequestedController implements Controller<InputMessage, Void> {
    private final Store store;
    private final Mapper<InputMessage, DesignDocumentDeleteRequested> inputMapper;
    private final Mapper<DesignDocumentDeleteCompleted, OutputMessage> outputMapper;
    private final KafkaEmitter emitter;

    public DesignDocumentDeleteRequestedController(Store store, Mapper<InputMessage, DesignDocumentDeleteRequested> inputMapper, Mapper<DesignDocumentDeleteCompleted, OutputMessage> outputMapper, KafkaEmitter emitter) {
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
                .flatMapSingle(emitter::onNext)
                .ignoreElements()
                .toCompletable()
                .toSingleDefault("")
                .map(result -> null);
    }

    private Observable<DesignDocumentDeleteCompleted> onDesignDocumentDeleteRequested(DesignDocumentDeleteRequested event) {
        return store.deleteDesign(new DeleteDesignRequest(event.getUuid()))
                .map(result -> new DesignDocumentDeleteCompleted(event.getEvid(), event.getUuid(), event.getEsid()))
                .toObservable();
    }
}
