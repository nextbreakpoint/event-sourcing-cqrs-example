package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.Controller;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.MessageEmitter;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.DesignAggregateUpdated;
import com.nextbreakpoint.blueprint.common.events.TilesRendered;
import com.nextbreakpoint.blueprint.designs.aggregate.DesignEventStore;
import com.nextbreakpoint.blueprint.designs.model.Design;
import rx.Observable;
import rx.Single;

import java.util.Objects;

public class TilesRenderedController implements Controller<InputMessage, Void> {
    private final Mapper<InputMessage, TilesRendered> inputMapper;
    private final Mapper<DesignAggregateUpdated, OutputMessage> outputMapper;
    private final MessageEmitter emitter;
    private final DesignEventStore eventStore;

    public TilesRenderedController(
            DesignEventStore eventStore,
            Mapper<InputMessage, TilesRendered> inputMapper,
            Mapper<DesignAggregateUpdated, OutputMessage> outputMapper,
            MessageEmitter emitter
    ) {
        this.eventStore = Objects.requireNonNull(eventStore);
        this.inputMapper = Objects.requireNonNull(inputMapper);
        this.outputMapper = Objects.requireNonNull(outputMapper);
        this.emitter = Objects.requireNonNull(emitter);
    }

    @Override
    public Single<Void> onNext(InputMessage message) {
        return Single.just(message)
                .flatMap(this::onMessageReceived)
                .map(inputMapper::transform)
                .flatMapObservable(event -> onUpdateRequested(event, message.getToken()))
                .ignoreElements()
                .toCompletable()
                .toSingleDefault("")
                .map(value -> null);
    }

    private Single<InputMessage> onMessageReceived(InputMessage message) {
        return eventStore.appendMessage(message).map(result -> message);
    }

    private Observable<Void> onUpdateRequested(TilesRendered event, String revision) {
        return updateDesign(event, revision).flatMap(this::sendUpdateEvents);
    }

    private Observable<Design> updateDesign(TilesRendered event, String revision) {
        return eventStore.projectDesign(event.getDesignId(), revision)
                .flatMapObservable(result -> result.map(Observable::just).orElseGet(Observable::empty))
                .flatMapSingle(eventStore::updateDesign)
                .flatMap(result -> result.map(Observable::just).orElseGet(Observable::empty));
    }

    private Observable<Void> sendUpdateEvents(Design design) {
        return createUpdateEvents(design)
                .map(outputMapper::transform)
                .flatMapSingle(emitter::send);
    }

    private Observable<DesignAggregateUpdated> createUpdateEvents(Design design) {
        return Observable.just(createUpdateEvent(design));
    }

    private DesignAggregateUpdated createUpdateEvent(Design design) {
        return DesignAggregateUpdated.builder()
                .withDesignId(design.getDesignId())
                .withCommandId(design.getCommandId())
                .withUserId(design.getUserId())
                .withRevision(design.getRevision())
                .withChecksum(design.getChecksum())
                .withData(design.getData())
                .withStatus(design.getStatus())
                .withPublished(design.isPublished())
                .withLevels(design.getLevels())
                .withBitmap(design.getBitmap())
                .withCreated(design.getCreated())
                .withUpdated(design.getUpdated())
                .build();
    }
}
