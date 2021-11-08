package com.nextbreakpoint.blueprint.common.vertx;

import com.nextbreakpoint.blueprint.common.core.Tombstone;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import io.vertx.rxjava.kafka.client.producer.KafkaProducerRecord;
import rx.Single;

import java.util.Objects;

public class TombstoneEmitter implements Controller<Tombstone, Void> {
    private final Logger logger = LoggerFactory.getLogger(TombstoneEmitter.class.getName());

    private final KafkaProducer<String, String> producer;
    private final String topic;
    private final int retries;

    public TombstoneEmitter(KafkaProducer<String, String> producer, String topic, int retries) {
        this.producer = Objects.requireNonNull(producer);
        this.topic = Objects.requireNonNull(topic);
        this.retries = retries;
    }

    @Override
    public Single<Void> onNext(Tombstone tombstone) {
        return Single.just(tombstone)
//                .doOnEach(action -> logger.debug("Sending tombstone: " + action.getValue()))
                .map(this::createKafkaRecord)
                .flatMap(this::writeRecord);
    }

    private Single<Void> writeRecord(KafkaProducerRecord<String, String> record) {
        return producer.rxWrite(record)
                .doOnError(err -> logger.error("Error occurred while writing record. Retrying...", err))
                .retry(retries);
    }

    private KafkaProducerRecord<String, String> createKafkaRecord(Tombstone tombstone) {
        return KafkaProducerRecord.create(topic, tombstone.getKey(), null);
    }
}
