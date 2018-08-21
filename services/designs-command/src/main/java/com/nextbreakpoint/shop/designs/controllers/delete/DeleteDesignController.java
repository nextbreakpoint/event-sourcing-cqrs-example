package com.nextbreakpoint.shop.designs.controllers.delete;

import com.nextbreakpoint.shop.common.model.Controller;
import com.nextbreakpoint.shop.common.model.Mapper;
import com.nextbreakpoint.shop.common.model.Message;
import com.nextbreakpoint.shop.common.model.events.DeleteDesignEvent;
import com.nextbreakpoint.shop.designs.model.DeleteDesignResult;
import io.vertx.core.json.Json;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import io.vertx.rxjava.kafka.client.producer.KafkaProducerRecord;
import rx.Single;

import java.util.Objects;

public class DeleteDesignController implements Controller<DeleteDesignEvent, DeleteDesignResult> {
    private Logger LOG = LoggerFactory.getLogger(DeleteDesignController.class);

    private final String topic;
    private final KafkaProducer<String, String> producer;
    private final Mapper<DeleteDesignEvent, Message> messageMapper;

    public DeleteDesignController(String topic, KafkaProducer<String, String> producer, Mapper<DeleteDesignEvent, Message> messageMapper) {
        this.topic = Objects.requireNonNull(topic);
        this.producer = Objects.requireNonNull(producer);
        this.messageMapper = Objects.requireNonNull(messageMapper);
    }

    @Override
    public Single<DeleteDesignResult> onNext(DeleteDesignEvent event) {
        return createRecord(event)
                .flatMap(record -> producer.rxWrite(record))
                .map(record -> new DeleteDesignResult(event.getUuid(), 1))
                .doOnError(err -> LOG.error("Failed to write record into Kafka", err))
                .onErrorReturn(err -> new DeleteDesignResult(event.getUuid(), 0));
    }

    protected Single<KafkaProducerRecord<String, String>> createRecord(DeleteDesignEvent request) {
        return Single.fromCallable(() -> KafkaProducerRecord.create(topic, request.getUuid().toString(), Json.encode(messageMapper.transform(request))));
    }
}
