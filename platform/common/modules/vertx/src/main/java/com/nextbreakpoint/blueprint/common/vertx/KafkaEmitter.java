package com.nextbreakpoint.blueprint.common.vertx;

import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.rxjava.kafka.client.producer.KafkaHeader;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import io.vertx.rxjava.kafka.client.producer.KafkaProducerRecord;
import rx.Single;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
                .doOnEach(notification -> logger.debug("Sending message: " + notification.getValue()))
                .map(this::createKafkaRecord)
                .map(record -> addHeaders(message, record))
                .flatMap(this::writeRecord);
    }

    private KafkaProducerRecord<String, String> addHeaders(OutputMessage message, KafkaProducerRecord<String, String> record) {
        record.addHeaders(makeHeaders(message));
        return record;
    }

    private List<KafkaHeader> makeHeaders(OutputMessage message) {
        return message.getTrace().toHeaders().entrySet().stream()
                .map(e -> KafkaHeader.header(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
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
