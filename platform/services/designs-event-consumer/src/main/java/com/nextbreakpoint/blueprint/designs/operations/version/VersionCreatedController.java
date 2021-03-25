package com.nextbreakpoint.blueprint.designs.operations.version;

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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class VersionCreatedController implements Controller<VersionCreated, ControllerResult> {
    private final Logger logger = LoggerFactory.getLogger(VersionCreatedController.class.getName());

    protected final int retries;
    private final Store store;
    private final String topic;
    private final KafkaProducer<String, String> producer;
    private final Mapper<RenderCreated, Message> mapper;

    public VersionCreatedController(Store store, String topic, KafkaProducer<String, String> producer, Mapper<RenderCreated, Message> mapper) {
        this.retries = 3;
        this.store = Objects.requireNonNull(store);
        this.topic = Objects.requireNonNull(topic);
        this.producer = Objects.requireNonNull(producer);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public Single<ControllerResult> onNext(VersionCreated event) {
        return Observable.from(createLevels())
                .flatMapSingle(level -> createLevel(event, level))
                .reduce(new ControllerResult(), (r1, r2) -> new ControllerResult())
                .toSingle()
                .doOnError(err -> logger.error("Can't create renders for version " + event.getUuid(), err))
                .onErrorReturn(ControllerResult::new);
    }

    private Single<ControllerResult> createLevel(VersionCreated event, short level) {
        return store.selectRender(event.getUuid(), level)
                .flatMap(result -> {
                    if (result.getValue().isEmpty()) {
                        return Single.just(createRender(event, level))
                                .flatMap(render -> store.insertRender(render).flatMap(versionResult -> publishRecordOrFailQuietly(render)));
                    }
                    return Single.just(new ControllerResult());
                })
                .doOnError(err -> logger.error("Can't create render for version " + event.getUuid() + ", level " + level, err))
                .onErrorReturn(ControllerResult::new);
    }

    private List<Short> createLevels() {
        return Arrays.asList((short)0, (short)1, (short)2);
    }

    private Single<ControllerResult> publishRecordOrFailQuietly(DesignRender render) {
        return createRecord(render)
                .flatMap(this::publishRecord)
                .flatMap(record -> onRecordSent(render))
                .doOnError(err -> logger.error("Can't send message. The operation will be retried later", err))
                .map(record -> new ControllerResult());
    }

    private Single<KafkaProducerRecord<String, String>> createRecord(DesignRender render) {
        return Single.fromCallable(() -> KafkaProducerRecord.create(topic, createKey(render), createValue(render)));
    }

    private String createKey(DesignRender render) {
        return render.getUuid().toString();
    }

    private String createValue(DesignRender render) {
        return Json.encode(mapper.transform(createEvent(render)));
    }

    private Single<KafkaProducerRecord<String, String>> publishRecord(KafkaProducerRecord<String, String> record) {
        return producer.rxWrite(record).retry(retries).map(ignore -> record);
    }

    private Single<PersistenceResult<Void>> onRecordSent(DesignRender render) {
        return store.publishRender(render.getUuid(), render.getVersion().getUuid(), render.getLevel());
    }

    private RenderCreated createEvent(DesignRender render) {
        return new RenderCreated(render.getVersion().getUuid(), render.getVersion().getData(), render.getVersion().getChecksum(), render.getLevel());
    }

    private DesignRender createRender(VersionCreated event, short level) {
        return new DesignRender(UUID.randomUUID(), level, new DesignVersion(event.getUuid(), event.getData(), event.getChecksum()));
    }
}
