package com.nextbreakpoint.blueprint.designs.operations;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.common.vertx.RecordAndEvent;
import com.nextbreakpoint.blueprint.designs.Store;
import com.nextbreakpoint.blueprint.designs.common.KafkaEmitter;
import com.nextbreakpoint.blueprint.designs.model.*;
import com.nextbreakpoint.blueprint.designs.events.TileRenderRequested;
import com.nextbreakpoint.blueprint.designs.events.VersionCreated;
import rx.Observable;
import rx.Single;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class VersionCreatedController<T extends RecordAndEvent<VersionCreated>> implements Controller<T, Void> {
    private final Mapper<TileRenderRequested, Message> mapper;
    private final KafkaEmitter emitter;
    private final Store store;

    public VersionCreatedController(Store store, Mapper<TileRenderRequested, Message> mapper, KafkaEmitter emitter) {
        this.store = Objects.requireNonNull(store);
        this.mapper = Objects.requireNonNull(mapper);
        this.emitter = Objects.requireNonNull(emitter);
    }

    @Override
    public Single<Void> onNext(T object) {
        return Observable.fromCallable(() -> new EventMetadata(object.getRecord(), Uuids.timeBased()))
                .flatMap(metadata ->
                    Observable.merge(
                        createTiles(object.getEvent(), (short) 0),
                        createTiles(object.getEvent(), (short) 1)
                    )
                )
                .map(mapper::transform)
                .flatMapSingle(emitter::onNext)
                .toSingle();
    }

    private Observable<TileRenderRequested> createTiles(VersionCreated event, short level) {
        return Observable.from(createTileHeaders(level))
                .flatMap(header -> insertTile(event, header));
    }

    private Observable<TileRenderRequested> insertTile(VersionCreated event, TileHeader header) {
        return Observable.fromCallable(() -> new DesignTile(event.getChecksum(), header.getLevel(), header.getX(), header.getY()))
                .flatMapSingle(store::insertDesignTile)
                .map(result -> new TileRenderRequested(event.getChecksum(), header.getLevel(), header.getX(), header.getY(), event.getData()));
    }

    private List<TileHeader> createTileHeaders(short level) {
        final int size = (int) Math.rint(Math.pow(2, level));
        return IntStream.range(0, size)
                .mapToObj(x -> (short) x)
                .flatMap(x ->
                        IntStream.range(0, size)
                                .mapToObj(y -> (short) y)
                                .map(y -> new TileHeader(level, x, y))
                )
                .collect(Collectors.toList());
    }
}
