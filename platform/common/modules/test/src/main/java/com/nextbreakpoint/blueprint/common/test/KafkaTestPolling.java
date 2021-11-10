package com.nextbreakpoint.blueprint.common.test;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Payload;
import io.vertx.core.json.Json;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumer;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumerRecords;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class KafkaTestPolling {
    private final List<InputMessage> messages = new ArrayList<>();

    private final KafkaConsumer<String, String> kafkaConsumer;

    public KafkaTestPolling(KafkaConsumer<String, String> kafkaConsumer) {
        this.kafkaConsumer = kafkaConsumer;
    }

    public List<InputMessage> findMessages(String partitionKey, String messageSource, String messageType) {
        return findMessages(messageSource, messageType, key -> key.equals(partitionKey));
    }

    public List<InputMessage> findMessages(String messageSource, String messageType, Predicate<String> keyPredicate) {
        synchronized (messages) {
            return messages.stream()
                    .filter(message -> keyPredicate.test(message.getKey()))
                    .filter(message -> message.getValue().getSource().equals(messageSource))
                    .filter(message -> message.getValue().getType().equals(messageType))
//                    .sorted(Comparator.comparing(Message::getTimestamp))
                    .collect(Collectors.toList());
        }
    }

    public void clearMessages() {
        synchronized (messages) {
            messages.clear();
        }
    }

    public void startPolling() {
        pollMessages();
    }

    private void appendMessage(InputMessage message) {
        synchronized (messages) {
            messages.add(message);
        }
    }

    private void consumeMessages(KafkaConsumerRecords<String, String> consumerRecords) {
        IntStream.range(0, consumerRecords.size())
                .forEach(index -> appendMessage(convertToMessage(consumerRecords.recordAt(index))));
    }

    private InputMessage convertToMessage(KafkaConsumerRecord<String, String> record) {
        return new InputMessage(record.key(), record.offset(), Json.decodeValue(record.value(), Payload.class), record.timestamp());
    }

    private void pollMessages() {
        kafkaConsumer.rxPoll(Duration.ofSeconds(5))
//                .doOnSuccess(records -> System.out.println("Received " + records.size() + " records"))
                .doOnSuccess(this::consumeMessages)
                .flatMap(result -> kafkaConsumer.rxCommit())
                .doOnError(Throwable::printStackTrace)
                .doAfterTerminate(this::pollMessages)
                .subscribe();
    }
}