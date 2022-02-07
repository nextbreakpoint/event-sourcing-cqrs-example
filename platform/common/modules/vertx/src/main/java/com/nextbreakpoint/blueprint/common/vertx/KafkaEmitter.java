package com.nextbreakpoint.blueprint.common.vertx;

import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import com.nextbreakpoint.blueprint.common.core.Json;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import io.vertx.rxjava.kafka.client.producer.KafkaProducerRecord;
import rx.Single;

import java.util.Objects;

public class KafkaEmitter implements Controller<OutputMessage, Void> {
    private final Logger logger = LoggerFactory.getLogger(KafkaEmitter.class.getName());

    private final KafkaProducer<String, String> producer;
    private final String topicName;
    private final int retries;

    public KafkaEmitter(KafkaProducer<String, String> producer, String topicName, int retries) {
        this.producer = Objects.requireNonNull(producer);
        this.topicName = Objects.requireNonNull(topicName);
        this.retries = retries;
    }

    @Override
    public Single<Void> onNext(OutputMessage message) {
        return Single.just(message)
//                .doOnEach(outputMessage -> logger.debug("Sending message: " + outputMessage))
                .map(this::createKafkaRecord)
                .flatMap(this::writeRecord);
    }

    private Single<Void> writeRecord(KafkaProducerRecord<String, String> record) {
        return producer.rxWrite(record)
                .doOnError(err -> logger.error("Error occurred while writing record. Retrying...", err))
                .retry(retries);
    }

    private KafkaProducerRecord<String, String> createKafkaRecord(OutputMessage message) {
        return KafkaProducerRecord.create(topicName, message.getKey(), Json.encodeValue(message.getValue()));
    }
}
