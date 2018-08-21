package com.nextbreakpoint.shop.designs.controllers.delete;

import com.nextbreakpoint.shop.common.model.Controller;
import com.nextbreakpoint.shop.common.model.Mapper;
import com.nextbreakpoint.shop.common.model.Message;
import com.nextbreakpoint.shop.common.model.events.DeleteDesignsEvent;
import com.nextbreakpoint.shop.designs.model.DeleteDesignsResult;
import io.vertx.core.json.Json;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import io.vertx.rxjava.kafka.client.producer.KafkaProducerRecord;
import rx.Single;

import java.util.Objects;
import java.util.UUID;

public class DeleteDesignsController implements Controller<DeleteDesignsEvent, DeleteDesignsResult> {
    private Logger LOG = LoggerFactory.getLogger(DeleteDesignsController.class);

    private final String topic;
    private final KafkaProducer<String, String> producer;
    private final Mapper<DeleteDesignsEvent, Message> messageMapper;

    public DeleteDesignsController(String topic, KafkaProducer<String, String> producer, Mapper<DeleteDesignsEvent, Message> messageMapper) {
        this.topic = Objects.requireNonNull(topic);
        this.producer = Objects.requireNonNull(producer);
        this.messageMapper = Objects.requireNonNull(messageMapper);
    }

    @Override
    public Single<DeleteDesignsResult> onNext(DeleteDesignsEvent event) {
        return createRecord(event)
                .flatMap(record -> producer.rxWrite(record))
                .map(record -> new DeleteDesignsResult(1))
                .doOnError(err -> LOG.error("Failed to write record into Kafka", err))
                .onErrorReturn(err -> new DeleteDesignsResult(0));
    }

    protected Single<KafkaProducerRecord<String, String>> createRecord(DeleteDesignsEvent request) {
        return Single.fromCallable(() -> KafkaProducerRecord.create(topic, new UUID(0,0).toString(), Json.encode(messageMapper.transform(request))));
    }
}
