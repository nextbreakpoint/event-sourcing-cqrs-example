package com.nextbreakpoint.blueprint.designs.common;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
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

public abstract class AbstractController<T extends Command> implements Controller<T, ControllerResult> {
    private final Logger logger = LoggerFactory.getLogger(AbstractController.class.getName());

    protected final int retries;
    protected final Store store;
    protected final String topic;
    protected final KafkaProducer<String, String> producer;
    protected final Mapper<DesignChanged, OutputMessage> mapper;

    public AbstractController(Store store, String topic, KafkaProducer<String, String> producer, Mapper<DesignChanged, OutputMessage> mapper, int retries) {
        this.retries = retries;
        this.store = Objects.requireNonNull(store);
        this.topic = Objects.requireNonNull(topic);
        this.producer = Objects.requireNonNull(producer);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public Single<ControllerResult> onNext(T command) {
        final UUID eventTimestamp = Uuids.timeBased();
        return executeCommand(command, eventTimestamp)
                .doOnError(err -> logger.error("Can't insert design", err))
                .flatMap(result -> updateAggregateOrFailQuietly(result, eventTimestamp));
    }

    private Single<ControllerResult> updateAggregateOrFailQuietly(PersistenceResult<Void> result, UUID eventTimestamp) {
        return store.updateDesignAggregate(result.getUuid(), eventTimestamp)
                .doOnError(err -> logger.error("Can't update aggregate. The operation will be retried later", err))
                .flatMap(aggregateResult -> publishRecordOrFailQuietly(aggregateResult.getUuid(), eventTimestamp, aggregateResult.getValue().orElseThrow()))
                .onErrorReturn(ControllerResult::new);
    }

    protected abstract Single<PersistenceResult<Void>> executeCommand(T command, UUID eventTimestamp);

    private Single<ControllerResult> publishRecordOrFailQuietly(UUID uuid, UUID eventTimestamp, DesignChange change) {
        return createRecord(change)
                .flatMap(this::publishRecord)
                .flatMap(record -> onRecordSent(uuid, eventTimestamp))
                .doOnError(err -> logger.error("Can't send message. The operation will be retried later", err))
                .map(record -> new ControllerResult());
    }

    private Single<KafkaProducerRecord<String, String>> createRecord(DesignChange change) {
        return Single.fromCallable(() -> KafkaProducerRecord.create(topic, createKey(change.getUuid()), createValue(change)));
    }

    private String createKey(UUID uuid) {
        return uuid.toString();
    }

    private String createValue(DesignChange change) {
        return Json.encode(mapper.transform(createEvent(change)));
    }

    private Single<KafkaProducerRecord<String, String>> publishRecord(KafkaProducerRecord<String, String> record) {
        return producer.rxWrite(record).retry(retries).map(ignore -> record);
    }

    private Single<PersistenceResult<Void>> onRecordSent(UUID uuid, UUID eventTimestamp) {
        return store.publishDesign(uuid, eventTimestamp);
    }

    private DesignChanged createEvent(DesignChange change) {
        return new DesignChanged(change.getUuid(), change.getJson(), change.getChecksum(), change.getModified().getTime());
    }
}
