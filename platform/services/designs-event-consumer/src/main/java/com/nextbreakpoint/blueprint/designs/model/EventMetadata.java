package com.nextbreakpoint.blueprint.designs.model;

import io.vertx.rxjava.kafka.client.consumer.KafkaConsumerRecord;

import java.util.UUID;

public class EventMetadata {
    private final KafkaConsumerRecord<String, String> record;
    private final UUID timeuuid;

    public EventMetadata(KafkaConsumerRecord<String, String> record, UUID timeuuid) {
        this.record = record;
        this.timeuuid = timeuuid;
    }

    public int getPartition() {
        return record.partition();
    }

    public long getOffset() {
        return record.offset();
    }

    public UUID getTimeUUID() {
        return timeuuid;
    }
}
