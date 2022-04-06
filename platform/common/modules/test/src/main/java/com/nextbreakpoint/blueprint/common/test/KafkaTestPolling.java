package com.nextbreakpoint.blueprint.common.test;

import com.nextbreakpoint.blueprint.common.core.*;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import rx.Single;
import rx.schedulers.Schedulers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class KafkaTestPolling {
    private final List<InputMessage> messages = new ArrayList<>();

    private final KafkaConsumer<String, String> kafkaConsumer;

    private Set<String> topicNames;

    public KafkaTestPolling(KafkaConsumer<String, String> kafkaConsumer, String topicName) {
        this(kafkaConsumer, Set.of(topicName));
    }

    public KafkaTestPolling(KafkaConsumer<String, String> kafkaConsumer, Set<String> topicNames) {
        this.kafkaConsumer = kafkaConsumer;
        this.topicNames = topicNames;
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
                    .collect(Collectors.toList());
        }
    }

    public void clearMessages() {
        synchronized (messages) {
            messages.clear();
        }
    }

    public void startPolling() {
        Single.fromCallable(() -> {
            kafkaConsumer.subscribe(topicNames);

            kafkaConsumer.seekToEnd(kafkaConsumer.assignment());

            return null;
        })
        .subscribeOn(Schedulers.newThread())
        .doAfterTerminate(this::pollMessages)
        .subscribe();
    }

    private void pollMessages() {
        Single.fromCallable(() -> {
            ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofSeconds(10));

            System.out.println("Received " + records.count() + " messages");

            consumeMessages(records);

            kafkaConsumer.commitSync();

            return null;
        })
        .subscribeOn(Schedulers.newThread())
        .doAfterTerminate(this::pollMessages)
        .subscribe();
    }

    private void consumeMessages(ConsumerRecords<String, String> consumerRecords) {
        StreamSupport.stream(consumerRecords.spliterator(), false)
                .filter(record -> record.value() != null)
                .map(this::convertToMessage)
                .peek(message -> System.out.println("Received message: " + message))
                .forEach(this::appendMessage);
    }

    private InputMessage convertToMessage(ConsumerRecord<String, String> record) {
        final Payload payload = Json.decodeValue(record.value(), Payload.class);

        final String token = Token.from(record.timestamp(), record.offset());

        final Tracing tracing = Tracing.of(UUID.randomUUID().toString(), UUID.randomUUID().toString());

        return new InputMessage(record.key(), token, payload, record.timestamp());
    }

    private void appendMessage(InputMessage message) {
        synchronized (messages) {
            messages.add(message);
        }
    }
}