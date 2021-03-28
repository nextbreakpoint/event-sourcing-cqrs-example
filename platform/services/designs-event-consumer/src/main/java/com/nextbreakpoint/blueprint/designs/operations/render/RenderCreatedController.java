package com.nextbreakpoint.blueprint.designs.operations.render;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.designs.Store;
import com.nextbreakpoint.blueprint.designs.model.*;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.Json;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import io.vertx.rxjava.kafka.client.producer.KafkaProducerRecord;
import rx.Observable;
import rx.Single;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RenderCreatedController implements Controller<RenderCreated, ControllerResult> {
    private final Logger logger = LoggerFactory.getLogger(RenderCreatedController.class.getName());

    protected final int retries;
    private final Store store;
    private final String topic;
    private final KafkaProducer<String, String> producer;
    private final Mapper<TileCreated, Message> mapper;

    public RenderCreatedController(Store store, String topic, KafkaProducer<String, String> producer, Mapper<TileCreated, Message> mapper) {
        this.retries = 3;
        this.store = Objects.requireNonNull(store);
        this.topic = Objects.requireNonNull(topic);
        this.producer = Objects.requireNonNull(producer);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public Single<ControllerResult> onNext(RenderCreated event) {
        return Observable.from(createTileHeaders(event.getLevel()))
                .flatMapSingle(header -> createTile(event, header))
                .reduce(new ControllerResult(), (r1, r2) -> new ControllerResult())
                .toSingle()
                .doOnError(err -> logger.error("Can't create tiles for version " + event.getUuid() + ", level " + event.getLevel(), err))
                .onErrorReturn(ControllerResult::new);
    }

    private Single<ControllerResult> createTile(RenderCreated event, TileHeader header) {
        return store.selectTile(event.getUuid(), header.getLevel(), header.getX(), header.getY())
                .flatMap(result -> {
                    if (result.getValue().isEmpty()) {
                        return Single.just(createTile(event, header.getX(), header.getY()))
                                .flatMap(tile -> store.insertTile(tile).flatMap(versionResult -> publishRecordOrFailQuietly(tile)));
                    }
                    return Single.just(new ControllerResult());
                })
                .doOnError(err -> logger.error("Can't create tile for version " + event.getUuid() + ", level " + header.getLevel() + ", col " + header.getX() + ", row " + header.getY() + ")", err))
                .onErrorReturn(ControllerResult::new);
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

    private Single<ControllerResult> publishRecordOrFailQuietly(DesignTile tile) {
        return createRecord(tile)
                .flatMap(this::publishRecord)
                .flatMap(record -> onRecordSent(tile))
                .doOnError(err -> logger.error("Can't send message. The operation will be retried later", err))
                .map(record -> new ControllerResult());
    }

    private Single<KafkaProducerRecord<String, String>> createRecord(DesignTile tile) {
        return Single.fromCallable(() -> KafkaProducerRecord.create(topic, createKey(tile), createValue(tile)));
    }

    private String createKey(DesignTile tile) {
        return tile.getUuid().toString();
    }

    private String createValue(DesignTile tile) {
        return Json.encode(mapper.transform(createEvent(tile)));
    }

    private Single<KafkaProducerRecord<String, String>> publishRecord(KafkaProducerRecord<String, String> record) {
        return producer.rxWrite(record).retry(retries).map(ignore -> record);
    }

    private Single<PersistenceResult<Void>> onRecordSent(DesignTile tile) {
        return store.publishTile(tile.getUuid(), tile.getVersion().getUuid(), tile.getLevel(), tile.getX(), tile.getY());
    }

    private TileCreated createEvent(DesignTile tile) {
        return new TileCreated(tile.getVersion().getUuid(), tile.getVersion().getData(), tile.getVersion().getChecksum(), tile.getLevel(), tile.getX(), tile.getY());
    }

    private DesignTile createTile(RenderCreated event, short x, short y) {
        return new DesignTile(UUID.randomUUID(), event.getLevel(), x, y, new DesignVersion(event.getUuid(), event.getData(), event.getChecksum()));
    }
}
