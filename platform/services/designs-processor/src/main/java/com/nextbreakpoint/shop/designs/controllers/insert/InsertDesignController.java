package com.nextbreakpoint.shop.designs.controllers.insert;

import com.nextbreakpoint.shop.common.model.Controller;
import com.nextbreakpoint.shop.common.model.Mapper;
import com.nextbreakpoint.shop.common.model.Message;
import com.nextbreakpoint.shop.common.model.commands.InsertDesignCommand;
import com.nextbreakpoint.shop.common.model.events.DesignChangedEvent;
import com.nextbreakpoint.shop.designs.Store;
import io.vertx.core.json.Json;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import io.vertx.rxjava.kafka.client.producer.KafkaProducerRecord;
import rx.Single;

import java.util.Objects;

public class InsertDesignController implements Controller<InsertDesignCommand, DesignChangedEvent> {
    private final Store store;
    private final String topic;
    private final KafkaProducer<String, String> producer;
    private final Mapper<DesignChangedEvent, Message> mapper;

    public InsertDesignController(Store store, String topic, KafkaProducer<String, String> producer, Mapper<DesignChangedEvent, Message> mapper) {
        this.store = Objects.requireNonNull(store);
        this.topic = Objects.requireNonNull(topic);
        this.producer = Objects.requireNonNull(producer);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public Single<DesignChangedEvent> onNext(InsertDesignCommand command) {
        return store.insertDesign(command)
                .map(result -> new DesignChangedEvent(result.getUuid(), System.currentTimeMillis()))
                .flatMap(this::sendMessageOrFail);
    }

    protected Single<DesignChangedEvent> sendMessageOrFail(DesignChangedEvent event) {
        return createRecord(event).flatMap(record -> producer.rxWrite(record).map(metadata -> event));
    }

    protected Single<KafkaProducerRecord<String, String>> createRecord(DesignChangedEvent event) {
        return Single.fromCallable(() -> KafkaProducerRecord.create(topic, event.getUuid().toString(), Json.encode(mapper.transform(event))));
    }
}
