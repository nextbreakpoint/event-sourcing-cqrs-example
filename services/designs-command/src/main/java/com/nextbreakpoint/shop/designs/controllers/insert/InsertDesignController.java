package com.nextbreakpoint.shop.designs.controllers.insert;

import com.nextbreakpoint.shop.common.model.Controller;
import com.nextbreakpoint.shop.common.model.Mapper;
import com.nextbreakpoint.shop.common.model.Message;
import com.nextbreakpoint.shop.common.model.events.InsertDesignEvent;
import com.nextbreakpoint.shop.designs.model.InsertDesignResult;
import io.vertx.core.json.Json;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import io.vertx.rxjava.kafka.client.producer.KafkaProducerRecord;
import rx.Single;

import java.util.Objects;

public class InsertDesignController implements Controller<InsertDesignEvent, InsertDesignResult> {
    private Logger LOG = LoggerFactory.getLogger(InsertDesignController.class);

    private final String topic;
    private final KafkaProducer<String, String> producer;
    private final Mapper<InsertDesignEvent, Message> messageMapper;

    public InsertDesignController(String topic, KafkaProducer<String, String> producer, Mapper<InsertDesignEvent, Message> messageMapper) {
        this.topic = Objects.requireNonNull(topic);
        this.producer = Objects.requireNonNull(producer);
        this.messageMapper = Objects.requireNonNull(messageMapper);
    }

    @Override
    public Single<InsertDesignResult> onNext(InsertDesignEvent event) {
        return createRecord(event)
                .flatMap(record -> producer.rxWrite(record))
                .map(record -> new InsertDesignResult(event.getUuid(), 1))
                .doOnError(err -> LOG.error("Failed to write record into Kafka", err))
                .onErrorReturn(err -> new InsertDesignResult(event.getUuid(), 0));
    }

    protected Single<KafkaProducerRecord<String, String>> createRecord(InsertDesignEvent request) {
        return Single.fromCallable(() -> KafkaProducerRecord.create(topic, request.getUuid().toString(), Json.encode(messageMapper.transform(request))));
    }
}
