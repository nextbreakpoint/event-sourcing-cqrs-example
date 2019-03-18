package com.nextbreakpoint.shop.designs.controllers.delete;

import com.nextbreakpoint.shop.common.model.Controller;
import com.nextbreakpoint.shop.common.model.events.DesignChangedEvent;
import com.nextbreakpoint.shop.common.vertx.DesignChangedMapper;
import com.nextbreakpoint.shop.designs.Store;
import com.nextbreakpoint.shop.designs.model.DeleteDesignRequest;
import com.nextbreakpoint.shop.designs.model.DeleteDesignResponse;
import io.vertx.core.json.Json;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import io.vertx.rxjava.kafka.client.producer.KafkaProducerRecord;
import rx.Single;

import java.util.Objects;

public class DeleteDesignController implements Controller<DeleteDesignRequest, DeleteDesignResponse> {
    private Logger logger = LoggerFactory.getLogger(DeleteDesignController.class);

    private final Store store;
    private final String topic;
    private final KafkaProducer<String, String> producer;
    private final DesignChangedMapper mapper;

    public DeleteDesignController(Store store, String topic, KafkaProducer<String, String> producer, DesignChangedMapper mapper) {
        this.store = Objects.requireNonNull(store);
        this.topic = Objects.requireNonNull(topic);
        this.producer = Objects.requireNonNull(producer);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public Single<DeleteDesignResponse> onNext(DeleteDesignRequest request) {
        return store.deleteDesign(request).flatMap(this::sendMessage);
    }

    protected Single<DeleteDesignResponse> sendMessage(DeleteDesignResponse response) {
        return createRecord(response)
                .flatMap(record -> producer.rxWrite(record).map(metadata -> response))
                .doOnError(err -> logger.error("Failed to write record into Kafka", err))
                .onErrorReturn(err -> response);
    }

    protected Single<KafkaProducerRecord<String, String>> createRecord(DeleteDesignResponse response) {
        return Single.fromCallable(() -> KafkaProducerRecord.create(topic, response.getUuid().toString(), Json.encode(mapper.transform(new DesignChangedEvent(response.getUuid(), System.currentTimeMillis())))));
    }
}
