package com.nextbreakpoint.blueprint.designs.common;

import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.Json;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import io.vertx.rxjava.kafka.client.producer.KafkaProducerRecord;
import rx.Single;

import java.util.Objects;

public class KafkaEmitter implements Controller<Message, Void> {
    private final Logger logger = LoggerFactory.getLogger(KafkaEmitter.class.getName());

    private final KafkaProducer<String, String> producer;
    private final String topic;
    private final int retries;

    public KafkaEmitter(KafkaProducer<String, String> producer, String topic, int retries) {
        this.producer = Objects.requireNonNull(producer);
        this.topic = Objects.requireNonNull(topic);
        this.retries = retries;
    }

    @Override
    public Single<Void> onNext(Message message) {
        return Single.just(message)
                .doOnEach(action -> logger.debug("Sending message: " + action.getValue()))
                .map(this::createKafkaRecord)
                .flatMap(this::writeRecord);
    }

    private Single<Void> writeRecord(KafkaProducerRecord<String, String> record) {
        return producer.rxWrite(record)
                .doOnError(err -> logger.error("Error occurred while writing record. Retrying...", err))
                .retry(retries);
    }

    private KafkaProducerRecord<String, String> createKafkaRecord(Message message) {
        return KafkaProducerRecord.create(topic, message.getPartitionKey(), Json.encode(message));
    }
}
