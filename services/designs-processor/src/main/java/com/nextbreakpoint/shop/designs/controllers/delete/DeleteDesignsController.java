package com.nextbreakpoint.shop.designs.controllers.delete;

import com.nextbreakpoint.shop.common.model.Controller;
import com.nextbreakpoint.shop.common.model.Mapper;
import com.nextbreakpoint.shop.common.model.Message;
import com.nextbreakpoint.shop.common.model.MessageReceipt;
import com.nextbreakpoint.shop.common.model.events.DeleteDesignsEvent;
import com.nextbreakpoint.shop.common.model.events.DesignChangedEvent;
import com.nextbreakpoint.shop.designs.Store;
import io.vertx.core.json.Json;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import io.vertx.rxjava.kafka.client.producer.KafkaProducerRecord;
import rx.Single;

import java.util.Objects;
import java.util.UUID;

public class DeleteDesignsController implements Controller<DeleteDesignsEvent, MessageReceipt> {
    private Logger LOG = LoggerFactory.getLogger(DeleteDesignsController.class);

    private final Store store;
    private final String topic;
    private final KafkaProducer<String, String> producer;
    private final Mapper<DesignChangedEvent, Message> messageMapper;

    public DeleteDesignsController(Store store, String topic, KafkaProducer<String, String> producer, Mapper<DesignChangedEvent, Message> messageMapper) {
        this.store = Objects.requireNonNull(store);
        this.topic = Objects.requireNonNull(topic);
        this.producer = Objects.requireNonNull(producer);
        this.messageMapper = Objects.requireNonNull(messageMapper);
    }

    @Override
    public Single<MessageReceipt> onNext(DeleteDesignsEvent event) {
        return store.deleteDesigns(event)
                .map(result -> new DesignChangedEvent(new UUID(0, 0), System.currentTimeMillis()))
                .flatMap(this::createRecord)
                .flatMap(record -> producer.rxWrite(record))
                .map(metadata -> new MessageReceipt(1, metadata.getTimestamp()))
                .doOnError(err -> LOG.error("Failed to write record into Kafka", err))
                .onErrorReturn(err -> new MessageReceipt(0, 0L));
    }

    protected Single<KafkaProducerRecord<String, String>> createRecord(DesignChangedEvent event) {
        return Single.fromCallable(() -> KafkaProducerRecord.create(topic, event.getUuid().toString(), Json.encode(messageMapper.transform(event))));
    }
}
