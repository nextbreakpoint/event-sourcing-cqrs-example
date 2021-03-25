package com.nextbreakpoint.blueprint.designs.operations.design;

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

import java.util.Objects;
import java.util.UUID;

public class DesignChangedController implements Controller<DesignChanged, ControllerResult> {
    private final Logger logger = LoggerFactory.getLogger(DesignChangedController.class.getName());

    protected final int retries;
    private final Store store;
    private final String topic;
    private final KafkaProducer<String, String> producer;
    private final Mapper<VersionCreated, Message> mapper;

    public DesignChangedController(Store store, String topic, KafkaProducer<String, String> producer, Mapper<VersionCreated, Message> mapper) {
        this.retries = 3;
        this.store = Objects.requireNonNull(store);
        this.topic = Objects.requireNonNull(topic);
        this.producer = Objects.requireNonNull(producer);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public Single<ControllerResult> onNext(DesignChanged event) {
        return store.selectVersion(event.getChecksum())
                .flatMap(result -> {
                    if (result.getValue().isEmpty()) {
                        return Single.just(createVersion(event))
                                .flatMap(version -> store.insertVersion(version).flatMap(versionResult -> publishRecordOrFailQuietly(version)));
                    }
                    return Single.just(new ControllerResult());
                })
                .doOnError(e -> logger.error("Can't create version for design " + event.getUuid(), e))
                .onErrorReturn(ControllerResult::new);
    }

    private Single<ControllerResult> publishRecordOrFailQuietly(DesignVersion version) {
        return createRecord(version)
                .flatMap(this::publishRecord)
                .flatMap(record -> onRecordSent(version))
                .doOnError(err -> logger.error("Can't send message. The operation will be retried later", err))
                .map(record -> new ControllerResult());
    }

    private Single<KafkaProducerRecord<String, String>> createRecord(DesignVersion version) {
        return Single.fromCallable(() -> KafkaProducerRecord.create(topic, createKey(version), createValue(version)));
    }

    private String createKey(DesignVersion version) {
        return version.getUuid().toString();
    }

    private String createValue(DesignVersion version) {
        return Json.encode(mapper.transform(createEvent(version)));
    }

    private Single<KafkaProducerRecord<String, String>> publishRecord(KafkaProducerRecord<String, String> record) {
        return producer.rxWrite(record).retry(retries).map(ignore -> record);
    }

    private Single<PersistenceResult<Void>> onRecordSent(DesignVersion version) {
        return store.publishVersion(version.getUuid(), version.getChecksum());
    }

    private VersionCreated createEvent(DesignVersion version) {
        return new VersionCreated(version.getUuid(), version.getData(), version.getChecksum());
    }

    private DesignVersion createVersion(DesignChanged event) {
        return new DesignVersion(UUID.randomUUID(), event.getJson(), event.getChecksum());
    }
}
