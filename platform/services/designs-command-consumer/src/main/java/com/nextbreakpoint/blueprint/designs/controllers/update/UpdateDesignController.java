package com.nextbreakpoint.blueprint.designs.controllers.update;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.core.command.UpdateDesign;
import com.nextbreakpoint.blueprint.common.core.event.DesignChanged;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.designs.Store;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.Json;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import io.vertx.rxjava.kafka.client.producer.KafkaProducerRecord;
import rx.Single;

import java.util.Objects;

public class UpdateDesignController implements Controller<UpdateDesign, DesignChanged> {
    private final Logger logger = LoggerFactory.getLogger(UpdateDesignController.class.getName());

    private final Store store;
    private final String topic;
    private final KafkaProducer<String, String> producer;
    private final Mapper<DesignChanged, Message> mapper;

    public UpdateDesignController(Store store, String topic, KafkaProducer<String, String> producer, Mapper<DesignChanged, Message> mapper) {
        this.store = Objects.requireNonNull(store);
        this.topic = Objects.requireNonNull(topic);
        this.producer = Objects.requireNonNull(producer);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public Single<DesignChanged> onNext(UpdateDesign command) {
        logger.info("Process update command: " + command.getUuid());
        return store.updateDesign(command)
                .map(result -> new DesignChanged(result.getUuid(), System.currentTimeMillis()))
                .flatMap(this::sendMessageOrFail)
                .doOnError(e -> logger.error("Can't send message", e));
    }

    protected Single<DesignChanged> sendMessageOrFail(DesignChanged event) {
        return createRecord(event).flatMap(record -> producer.rxWrite(record).map(metadata -> event));
    }

    protected Single<KafkaProducerRecord<String, String>> createRecord(DesignChanged event) {
        return Single.fromCallable(() -> KafkaProducerRecord.create(topic, event.getUuid().toString(), Json.encode(mapper.transform(event))));
    }
}
