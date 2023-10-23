package com.nextbreakpoint.blueprint.common.test;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.Payload;
import com.nextbreakpoint.blueprint.common.core.Token;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class KafkaTestPolling {
    private final List<InputMessage> messages = new ArrayList<>();

    private final KafkaConsumer<String, String> kafkaConsumer;

    private Set<String> topicNames;

    private Thread pollingThread;

    public KafkaTestPolling(KafkaConsumer<String, String> kafkaConsumer, String topicName) {
        this(kafkaConsumer, Set.of(topicName));
    }

    public KafkaTestPolling(KafkaConsumer<String, String> kafkaConsumer, Set<String> topicNames) {
        this.kafkaConsumer = kafkaConsumer;
        this.topicNames = topicNames;
    }

    public List<InputMessage> findMessages(String messageSource, String messageType, String partitionKey) {
        return findMessages(messageSource, messageType, key -> key.equals(partitionKey));
    }

    public List<InputMessage> findMessages(String messageSource, String messageType, Predicate<String> keyPredicate) {
        return findMessages(messageSource, messageType, keyPredicate, (message) -> true);
    }

    public List<InputMessage> findMessages(String messageSource, String messageType, Predicate<String> keyPredicate, Predicate<InputMessage> messagePredicate) {
        synchronized (messages) {
            return messages.stream()
                    .filter(message -> message.getValue().getSource().equals(messageSource))
                    .filter(message -> message.getValue().getType().equals(messageType))
                    .filter(message -> keyPredicate.test(message.getKey()))
                    .filter(messagePredicate::test)
                    .collect(Collectors.toList());
        }
    }

    public void clearMessages() {
        synchronized (messages) {
            messages.clear();
        }
    }

    public void startPolling() {
        if (pollingThread != null) {
            return;
        }

        pollingThread = new Thread(() -> {
            kafkaConsumer.subscribe(topicNames);

            kafkaConsumer.seekToEnd(kafkaConsumer.assignment());

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofSeconds(10));

                    System.out.println("Received " + records.count() + " messages");

                    consumeMessages(records);

                    kafkaConsumer.commitSync();
                } catch (Exception e) {
                    kafkaConsumer.pause(kafkaConsumer.assignment());

                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }

                    kafkaConsumer.seekToBeginning(kafkaConsumer.assignment());

                    kafkaConsumer.resume(kafkaConsumer.assignment());
                }
            }
        }, kafkaConsumer.groupMetadata().groupId());

        pollingThread.start();
    }

    public void stopPolling() {
        if (pollingThread != null) {
            try {
                pollingThread.interrupt();

                pollingThread.join();
            } catch (InterruptedException e) {
            }
        }
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

        return new InputMessage(record.key(), token, payload, record.timestamp());
    }

    private void appendMessage(InputMessage message) {
        synchronized (messages) {
            messages.add(message);
        }
    }
}