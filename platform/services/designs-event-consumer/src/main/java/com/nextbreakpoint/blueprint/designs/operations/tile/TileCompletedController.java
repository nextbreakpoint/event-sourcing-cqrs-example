package com.nextbreakpoint.blueprint.designs.operations.tile;

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
import rx.Single;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class TileCompletedController implements Controller<TileCompleted, ControllerResult> {
    private final Logger logger = LoggerFactory.getLogger(TileCompletedController.class.getName());

    private final int retries;
    private final Store store;
    private final String topic;
    private final KafkaProducer<String, String> producer;
    private final Mapper<RenderCompleted, Message> mapper;

    public TileCompletedController(Store store, String topic, KafkaProducer<String, String> producer, Mapper<RenderCompleted, Message> mapper) {
        this.retries = 3;
        this.store = Objects.requireNonNull(store);
        this.topic = Objects.requireNonNull(topic);
        this.producer = Objects.requireNonNull(producer);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public Single<ControllerResult> onNext(TileCompleted event) {
        return store.selectTile(event.getUuid(), event.getLevel(), event.getX(), event.getY())
                .flatMap(this::processTile)
                .flatMap(ignore -> store.selectRender(event.getUuid(), event.getLevel()))
                .flatMap(this::processRender)
                .flatMap(ignore -> store.selectTiles(event.getUuid(), event.getLevel()))
                .flatMap(tiles -> processTiles(tiles, event.getLevel()))
                .map(ignore -> createEvent(event))
                .flatMap(this::publishRecordOrFailQuietly)
                .doOnError(err -> logger.warn(err.getMessage()))
                .onErrorReturn(ControllerResult::new);
    }

    private Single<ControllerResult> processRender(PersistenceResult<RenderDocument> result) {
        return result.getValue().filter(document -> document.getCompleted() == null).map(ignore -> Single.just(new ControllerResult())).orElseGet(() -> Single.error(new RuntimeException("Render is already completed")));
    }

    private Single<ControllerResult> processTile(PersistenceResult<TileDocument> result) {
        return result.getValue().map(this::completeTile).map(single -> single.map(ignore -> new ControllerResult())).orElseGet(() -> Single.error(new RuntimeException("Can't find tile")));
    }

    private Single<PersistenceResult<Void>> completeTile(TileDocument tileDocument) {
        return store.completeTile(UUID.fromString(tileDocument.getUuid()), UUID.fromString(tileDocument.getVersion()), tileDocument.getLevel(), tileDocument.getX(), tileDocument.getY());
    }

    private Single<ControllerResult> processTiles(List<TileDocument> tiles, short level) {
        final int size = (int) Math.rint(Math.pow(2, level));
        if (tiles.size() != size * size || tiles.stream().anyMatch(tile -> tile.getCompleted() == null)) {
            return Single.error(new RuntimeException("Render is not completed"));
        } else {
            return Single.just(new ControllerResult());
        }
    }

    private Single<KafkaProducerRecord<String, String>> createRecord(RenderCompleted event) {
        return Single.fromCallable(() -> KafkaProducerRecord.create(topic, createKey(event), createValue(event)));
    }

    private String createKey(RenderCompleted event) {
        return event.getUuid().toString();
    }

    private String createValue(RenderCompleted event) {
        return Json.encode(mapper.transform(event));
    }

    private Single<ControllerResult> publishRecordOrFailQuietly(RenderCompleted event) {
        return createRecord(event)
                .flatMap(this::publishRecord)
                .flatMap(record -> onRecordSent(event))
                .doOnError(err -> logger.error("Can't send message. The operation will be retried later", err))
                .map(record -> new ControllerResult());
    }

    private Single<KafkaProducerRecord<String, String>> publishRecord(KafkaProducerRecord<String, String> record) {
        return producer.rxWrite(record).retry(retries).map(ignore -> record);
    }

    private Single<PersistenceResult<Void>> onRecordSent(RenderCompleted event) {
        return store.completeRender(event.getUuid(), event.getUuid(), event.getLevel());
    }

    private RenderCompleted createEvent(TileCompleted event) {
        return new RenderCompleted(event.getUuid(), event.getLevel());
    }
}
