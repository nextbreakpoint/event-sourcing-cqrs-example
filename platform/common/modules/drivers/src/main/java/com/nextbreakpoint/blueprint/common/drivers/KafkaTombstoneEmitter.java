package com.nextbreakpoint.blueprint.common.drivers;

import com.nextbreakpoint.blueprint.common.core.Tombstone;
import com.nextbreakpoint.blueprint.common.core.TombstoneEmitter;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import rx.Single;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Log4j2
public class KafkaTombstoneEmitter implements TombstoneEmitter {
    private final KafkaProducer<String, String> producer;
    private final String topicName;
    private final int retries;

    public KafkaTombstoneEmitter(KafkaProducer<String, String> producer, String topicName, int retries) {
        this.producer = Objects.requireNonNull(producer);
        this.topicName = Objects.requireNonNull(topicName);
        this.retries = retries;
    }

    @Override
    public Single<Void> send(Tombstone tombstone) {
        return send(tombstone, topicName);
    }

    @Override
    public Single<Void> send(Tombstone tombstone, String topicName) {
        return Single.just(tombstone)
                .doOnEach(notification -> log.debug("Sending tombstone to topic " + topicName + ": " + notification.getValue()))
                .map(this::writeRecord)
                .doOnError(err -> log.error("Error occurred while writing record. Retrying...", err))
                .retry(retries)
                .map(result -> null);
    }

    private RecordMetadata writeRecord(Tombstone tombstone) {
        try {
            return producer.send(createRecord(tombstone)).get(2000, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ProducerRecord<String, String> createRecord(Tombstone tombstone) {
        return new ProducerRecord<>(topicName, tombstone.getKey(), null);
    }

    @Override
    public String getTopicName() {
        return topicName;
    }
}
