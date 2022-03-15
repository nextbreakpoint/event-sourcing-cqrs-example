package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentUpdateCompleted;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentUpdateRequested;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.common.vertx.MessageEmitter;
import com.nextbreakpoint.blueprint.designs.Store;
import com.nextbreakpoint.blueprint.designs.model.Design;
import com.nextbreakpoint.blueprint.designs.persistence.dto.InsertDesignRequest;
import rx.Observable;
import rx.Single;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

public class DesignDocumentUpdateRequestedController implements Controller<InputMessage, Void> {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    private final Store store;
    private final Mapper<InputMessage, DesignDocumentUpdateRequested> inputMapper;
    private final MessageMapper<DesignDocumentUpdateCompleted, OutputMessage> outputMapper;
    private final MessageEmitter emitter;

    public DesignDocumentUpdateRequestedController(Store store, Mapper<InputMessage, DesignDocumentUpdateRequested> inputMapper, MessageMapper<DesignDocumentUpdateCompleted, OutputMessage> outputMapper, MessageEmitter emitter) {
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
                .flatMapSingle(emitter::send)
                .ignoreElements()
                .toCompletable()
                .toSingleDefault("")
                .map(result -> null);
    }

    private Observable<DesignDocumentUpdateCompleted> onDesignDocumentUpdateRequested(DesignDocumentUpdateRequested event) {
        return store.insertDesign(new InsertDesignRequest(event.getDesignId(), createDesign(event), true))
                .flatMap(result -> isCompleted(event) ? store.insertDesign(new InsertDesignRequest(event.getDesignId(), createDesign(event), false)) : Single.just(null))
                .map(result -> new DesignDocumentUpdateCompleted(event.getDesignId(), event.getRevision()))
                .toObservable();
    }

    private boolean isCompleted(DesignDocumentUpdateRequested event) {
        return event.getLevels() == 8 && completedTiles(event.getTiles()) == 21845;
    }

    private int completedTiles(List<Tiles> tiles) {
        return tiles.stream().mapToInt(Tiles::getCompleted).sum();
    }

    private Design createDesign(DesignDocumentUpdateRequested event) {
        return Design.builder()
                .withDesignId(event.getDesignId())
                .withUserId(event.getUserId())
                .withCommandId(event.getCommandId())
                .withChecksum(event.getChecksum())
                .withRevision(event.getRevision())
                .withData(event.getData())
                .withStatus(event.getStatus())
                .withLevels(event.getLevels())
                .withTiles(event.getTiles())
                .withLastModified(FORMATTER.format(event.getModified()))
                .build();
    }
}
