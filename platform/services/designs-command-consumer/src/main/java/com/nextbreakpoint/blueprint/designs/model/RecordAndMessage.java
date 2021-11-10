package com.nextbreakpoint.blueprint.designs.model;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumerRecord;

import java.util.Objects;

public class RecordAndMessage {
    private final KafkaConsumerRecord<String, String> record;
    private final InputMessage message;

    public RecordAndMessage(KafkaConsumerRecord<String, String> record, InputMessage message) {
        this.record = Objects.requireNonNull(record);
        this.message = Objects.requireNonNull(message);
    }

    public KafkaConsumerRecord<String, String> getRecord() {
        return record;
    }

    public InputMessage getMessage() {
        return message;
    }
}
