package com.nextbreakpoint.blueprint.common.test;

import com.nextbreakpoint.blueprint.common.core.*;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumer;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumerRecords;
import rx.schedulers.Schedulers;

import java.time.Duration;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
        kafkaConsumer.rxSubscribe(topicNames)
                .flatMap(ignore -> kafkaConsumer.rxPoll(Duration.ofSeconds(5)))
                .flatMap(records -> kafkaConsumer.rxAssignment())
                .flatMap(kafkaConsumer::rxSeekToEnd)
                .doOnError(Throwable::printStackTrace)
                .subscribeOn(Schedulers.io())
                .toBlocking()
                .value();

        kafkaConsumer.rxPoll(Duration.ofSeconds(10))
                .doOnSuccess(records -> System.out.println("Received " + records.size() + " messages"))
                .doOnSuccess(this::consumeMessages)
                .flatMap(result -> kafkaConsumer.rxCommit())
                .doOnError(Throwable::printStackTrace)
                .doAfterTerminate(this::pollMessages)
                .subscribeOn(Schedulers.io())
                .toBlocking()
                .value();
    }

    private void appendMessage(InputMessage message) {
        synchronized (messages) {
            messages.add(message);
        }
    }

    private void consumeMessages(KafkaConsumerRecords<String, String> consumerRecords) {
        IntStream.range(0, consumerRecords.size())
                .mapToObj(consumerRecords::recordAt)
                .filter(record -> record.value() != null)
                .map(this::convertToMessage)
                .peek(message -> System.out.println("Received message: " + message))
                .forEach(this::appendMessage);
    }

    private InputMessage convertToMessage(KafkaConsumerRecord<String, String> record) {
        final Payload payload = Json.decodeValue(record.value(), Payload.class);

        final String token = Token.from(record.timestamp(), record.offset());

        final Tracing tracing = Tracing.of(UUID.randomUUID().toString(), UUID.randomUUID().toString());

        return new InputMessage(record.key(), token, payload, tracing, record.timestamp());
    }

    private void pollMessages() {
        kafkaConsumer.rxPoll(Duration.ofSeconds(10))
                .doOnSuccess(records -> System.out.println("Received " + records.size() + " messages"))
                .doOnSuccess(this::consumeMessages)
                .flatMap(result -> kafkaConsumer.rxCommit())
                .doOnError(Throwable::printStackTrace)
                .doAfterTerminate(this::pollMessages)
                .subscribe();
    }
}