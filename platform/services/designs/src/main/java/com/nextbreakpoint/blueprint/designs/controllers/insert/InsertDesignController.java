package com.nextbreakpoint.blueprint.designs.controllers.insert;

import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.common.core.event.DesignChanged;
import com.nextbreakpoint.blueprint.common.vertx.DesignChangedMapper;
import com.nextbreakpoint.blueprint.designs.Store;
import com.nextbreakpoint.blueprint.designs.model.InsertDesignRequest;
import com.nextbreakpoint.blueprint.designs.model.InsertDesignResponse;
import io.vertx.core.json.Json;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import io.vertx.rxjava.kafka.client.producer.KafkaProducerRecord;
import rx.Single;

import java.util.Objects;

public class InsertDesignController implements Controller<InsertDesignRequest, InsertDesignResponse> {
    private Logger logger = LoggerFactory.getLogger(InsertDesignController.class);

    private final Store store;
    private final String topic;
    private final KafkaProducer<String, String> producer;
    private final DesignChangedMapper mapper;

    public InsertDesignController(Store store, String topic, KafkaProducer<String, String> producer, DesignChangedMapper mapper) {
        this.store = Objects.requireNonNull(store);
        this.topic = Objects.requireNonNull(topic);
        this.producer = Objects.requireNonNull(producer);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public Single<InsertDesignResponse> onNext(InsertDesignRequest request) {
        return store.insertDesign(request).flatMap(this::sendMessage);
    }

    protected Single<InsertDesignResponse> sendMessage(InsertDesignResponse response) {
        return createRecord(response)
                .flatMap(record -> producer.rxWrite(record).map(metadata -> response))
                .doOnError(err -> logger.error("Failed to write record into Kafka", err))
                .onErrorReturn(err -> response);
    }

    protected Single<KafkaProducerRecord<String, String>> createRecord(InsertDesignResponse response) {
        return Single.fromCallable(() -> KafkaProducerRecord.create(topic, response.getUuid().toString(), Json.encode(mapper.transform(new DesignChanged(response.getUuid(), System.currentTimeMillis())))));
    }
}
