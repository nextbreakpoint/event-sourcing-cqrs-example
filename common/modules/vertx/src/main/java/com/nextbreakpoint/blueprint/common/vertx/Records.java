package com.nextbreakpoint.blueprint.common.vertx;

import com.nextbreakpoint.blueprint.common.core.Header;
import com.nextbreakpoint.blueprint.common.core.InputRecord;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.OutputRecord;
import com.nextbreakpoint.blueprint.common.core.MessagePayload;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Records {
    private Records() {}

    public static Mapper<ConsumerRecord<String, com.nextbreakpoint.blueprint.common.commands.avro.Payload>, InputRecord<Object>> createCommandInputRecordMapper() {
        return record -> InputRecord.builder()
                .withKey(record.key())
                .withTopicName(record.topic())
                .withPartition(record.partition())
                .withOffset(record.offset())
                .withHeaders(createHeaders(record.headers()))
                .withPayloadV2(createCommandPayload(record))
                .withTimestamp(record.timestamp())
                .build();
    }

    public static Mapper<ConsumerRecord<String, com.nextbreakpoint.blueprint.common.events.avro.Payload>, InputRecord<Object>> createEventInputRecordMapper() {
        return record -> InputRecord.builder()
                .withKey(record.key())
                .withTopicName(record.topic())
                .withPartition(record.partition())
                .withOffset(record.offset())
                .withHeaders(createHeaders(record.headers()))
                .withPayloadV2(createEventPayload(record))
                .withTimestamp(record.timestamp())
                .build();
    }

    public static <T> Mapper<OutputRecord<T>, ProducerRecord<String, com.nextbreakpoint.blueprint.common.commands.avro.Payload>> createCommandOutputRecordMapper() {
        return outputRecord -> new ProducerRecord<>(
                outputRecord.getTopicName(),
                null,
                outputRecord.getKey(),
                createCommandPayload(outputRecord),
                createHeaders(outputRecord)
        );
    }

    public static <T> Mapper<OutputRecord<T>, ProducerRecord<String, com.nextbreakpoint.blueprint.common.events.avro.Payload>> createEventOutputRecordMapper() {
        return outputRecord -> new ProducerRecord<>(
                outputRecord.getTopicName(),
                null,
                outputRecord.getKey(),
                createEventPayload(outputRecord),
                createHeaders(outputRecord)
        );
    }

    private static MessagePayload<Object> createCommandPayload(ConsumerRecord<String, com.nextbreakpoint.blueprint.common.commands.avro.Payload> record) {
        return MessagePayload.builder()
                .withUuid(record.value().getUuid())
                .withType(record.value().getType())
                .withSource(record.value().getSource())
                .withData(record.value().getCommand())
                .build();
    }

    private static MessagePayload<Object> createEventPayload(ConsumerRecord<String, com.nextbreakpoint.blueprint.common.events.avro.Payload> record) {
        return MessagePayload.builder()
                .withUuid(record.value().getUuid())
                .withType(record.value().getType())
                .withSource(record.value().getSource())
                .withData(record.value().getEvent())
                .build();
    }

    private static <T> com.nextbreakpoint.blueprint.common.commands.avro.Payload createCommandPayload(OutputRecord<T> outputRecord) {
        return com.nextbreakpoint.blueprint.common.commands.avro.Payload.newBuilder()
                .setUuid(outputRecord.getPayloadV2().getUuid())
                .setType(outputRecord.getPayloadV2().getType())
                .setSource(outputRecord.getPayloadV2().getSource())
                .setCommand(outputRecord.getPayloadV2().getData())
                .build();
    }

    private static <T> com.nextbreakpoint.blueprint.common.events.avro.Payload createEventPayload(OutputRecord<T> outputRecord) {
        return com.nextbreakpoint.blueprint.common.events.avro.Payload.newBuilder()
                .setUuid(outputRecord.getPayloadV2().getUuid())
                .setType(outputRecord.getPayloadV2().getType())
                .setSource(outputRecord.getPayloadV2().getSource())
                .setEvent(outputRecord.getPayloadV2().getData())
                .build();
    }

    private static List<Header> createHeaders(Headers headers) {
        return StreamSupport.stream(headers.spliterator(), false).map(Records::createHeader).toList();
    }

    private static Header createHeader(org.apache.kafka.common.header.Header header) {
        return Header.builder().withKey(header.key()).withValue(new String(header.value(), UTF_8)).build();
    }

    private static List<org.apache.kafka.common.header.Header> createHeaders(OutputRecord<?> record) {
        return record.getHeaders().stream().map(Records::createHeader).collect(Collectors.toList());
    }

    private static RecordHeader createHeader(Header header) {
        return new RecordHeader(header.getKey(), header.getValue().getBytes());
    }
}
