package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentUpdateCompleted;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentUpdateRequested;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.common.vertx.KafkaEmitter;
import com.nextbreakpoint.blueprint.designs.Store;
import com.nextbreakpoint.blueprint.designs.model.Design;
import com.nextbreakpoint.blueprint.designs.model.Tiles;
import com.nextbreakpoint.blueprint.designs.persistence.InsertDesignRequest;
import rx.Observable;
import rx.Single;

import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.stream.Collectors;

public class DesignDocumentUpdateRequestedController implements Controller<InputMessage, Void> {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_INSTANT;

    private final Store store;
    private final Mapper<InputMessage, DesignDocumentUpdateRequested> inputMapper;
    private final Mapper<DesignDocumentUpdateCompleted, OutputMessage> outputMapper;
    private final KafkaEmitter emitter;

    public DesignDocumentUpdateRequestedController(Store store, Mapper<InputMessage, DesignDocumentUpdateRequested> inputMapper, Mapper<DesignDocumentUpdateCompleted, OutputMessage> outputMapper, KafkaEmitter emitter) {
        this.store = Objects.requireNonNull(store);
        this.inputMapper = Objects.requireNonNull(inputMapper);
        this.outputMapper = Objects.requireNonNull(outputMapper);
        this.emitter = Objects.requireNonNull(emitter);
    }

    @Override
    public Single<Void> onNext(InputMessage message) {
        return Single.just(message)
                .map(inputMapper::transform)
                .flatMapObservable(this::onDesignDocumentUpdateRequested)
                .map(outputMapper::transform)
                .flatMapSingle(emitter::onNext)
                .ignoreElements()
                .toCompletable()
                .toSingleDefault("")
                .map(result -> null);
    }

    private Observable<DesignDocumentUpdateCompleted> onDesignDocumentUpdateRequested(DesignDocumentUpdateRequested event) {
        return store.insertDesign(new InsertDesignRequest(event.getUuid(), createDesign(event)))
                .map(result -> new DesignDocumentUpdateCompleted(event.getUuid(), event.getEvid()))
                .toObservable();
    }

    private Design createDesign(DesignDocumentUpdateRequested event) {
        return Design.builder()
                .withEvid(event.getEvid())
                .withUuid(event.getUuid())
                .withEsid(event.getEsid())
                .withJson(event.getJson())
                .withChecksum(event.getChecksum())
                .withStatus(event.getStatus())
                .withLevels(event.getLevels())
                .withTiles(event.getTiles().stream().map(this::createTiles).collect(Collectors.toList()))
                .withModified(FORMATTER.format(event.getModified().toInstant()))
                .build();
    }

    private Tiles createTiles(DesignDocumentUpdateRequested.Tiles tiles) {
        return Tiles.builder()
                .withLevel(tiles.getLevel())
                .withRequested(tiles.getRequested())
                .withCompleted(tiles.getCompleted())
                .withFailed(tiles.getFailed())
                .build();
    }
}
