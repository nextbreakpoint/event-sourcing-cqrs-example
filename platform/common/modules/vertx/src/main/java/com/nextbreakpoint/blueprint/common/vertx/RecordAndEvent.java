package com.nextbreakpoint.blueprint.common.vertx;

import io.vertx.rxjava.kafka.client.consumer.KafkaConsumerRecord;

import java.util.Objects;

public class RecordAndEvent<T> {
    private final KafkaConsumerRecord<String, String> record;
    private final T event;

    public RecordAndEvent(KafkaConsumerRecord<String, String> record, T event) {
        this.record = Objects.requireNonNull(record);
        this.event = Objects.requireNonNull(event);
    }

    public KafkaConsumerRecord<String, String> getRecord() {
        return record;
    }

    public T getEvent() {
        return event;
    }
}
