package com.nextbreakpoint.shop.designs.controllers.update;

import com.nextbreakpoint.shop.common.model.Controller;
import com.nextbreakpoint.shop.common.model.Mapper;
import com.nextbreakpoint.shop.common.model.Message;
import com.nextbreakpoint.shop.common.model.events.UpdateDesignEvent;
import com.nextbreakpoint.shop.designs.model.UpdateDesignResult;
import io.vertx.core.json.Json;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import io.vertx.rxjava.kafka.client.producer.KafkaProducerRecord;
import rx.Single;

import java.util.Objects;

public class UpdateDesignController implements Controller<UpdateDesignEvent, UpdateDesignResult> {
    private Logger LOG = LoggerFactory.getLogger(UpdateDesignController.class);

    private final String topic;
    private final KafkaProducer<String, String> producer;
    private final Mapper<UpdateDesignEvent, Message> messageMapper;

    public UpdateDesignController(String topic, KafkaProducer<String, String> producer, Mapper<UpdateDesignEvent, Message> messageMapper) {
        this.topic = Objects.requireNonNull(topic);
        this.producer = Objects.requireNonNull(producer);
        this.messageMapper = Objects.requireNonNull(messageMapper);
    }

    @Override
    public Single<UpdateDesignResult> onNext(UpdateDesignEvent event) {
        return createRecord(event)
                .flatMap(record -> producer.rxWrite(record))
                .map(record -> new UpdateDesignResult(event.getUuid(), 1))
                .doOnError(err -> LOG.error("Failed to write record into Kafka", err))
                .onErrorReturn(err -> new UpdateDesignResult(event.getUuid(), 0));
    }

    protected Single<KafkaProducerRecord<String, String>> createRecord(UpdateDesignEvent request) {
        return Single.fromCallable(() -> KafkaProducerRecord.create(topic, request.getUuid().toString(), Json.encode(messageMapper.transform(request))));
    }
}
