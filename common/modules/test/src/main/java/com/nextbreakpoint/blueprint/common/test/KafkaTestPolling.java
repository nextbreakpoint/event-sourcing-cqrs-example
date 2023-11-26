package com.nextbreakpoint.blueprint.common.test;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.InputRecord;
import com.nextbreakpoint.blueprint.common.core.Mapper;
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

public class KafkaTestPolling<T, R> {
    private final List<InputMessage<T>> messages = new ArrayList<>();

    private final KafkaConsumer<String, R> kafkaConsumer;

    private final Mapper<ConsumerRecord<String, R>, InputRecord<T>> recordMapper;

    private final Set<String> topicNames;

    private Thread pollingThread;

    public KafkaTestPolling(KafkaConsumer<String, R> kafkaConsumer, Mapper<ConsumerRecord<String, R>, InputRecord<T>> recordMapper, String topicName) {
        this(kafkaConsumer, recordMapper, Set.of(topicName));
    }

    public KafkaTestPolling(KafkaConsumer<String, R> kafkaConsumer, Mapper<ConsumerRecord<String, R>, InputRecord<T>> recordMapper, Set<String> topicNames) {
        this.kafkaConsumer = kafkaConsumer;
        this.recordMapper = recordMapper;
        this.topicNames = topicNames;
    }

    public List<InputMessage<T>> findMessages(String messageSource, String messageType, String partitionKey) {
        return findMessages(messageSource, messageType, key -> key.equals(partitionKey));
    }

    public List<InputMessage<T>> findMessages(String messageSource, String messageType, Predicate<String> keyPredicate) {
        return findMessages(messageSource, messageType, keyPredicate, (message) -> true);
    }

    public List<InputMessage<T>> findMessages(String messageSource, String messageType, Predicate<String> keyPredicate, Predicate<InputMessage<T>> messagePredicate) {
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
                    ConsumerRecords<String, R> records = kafkaConsumer.poll(Duration.ofSeconds(10));

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

    private void consumeMessages(ConsumerRecords<String, R> consumerRecords) {
        StreamSupport.stream(consumerRecords.spliterator(), false)
                .filter(record -> record.value() != null)
                .map(this::convertToMessage)
                .peek(message -> System.out.println("Received message: " + message))
                .forEach(this::appendMessage);
    }

    private InputMessage<T> convertToMessage(ConsumerRecord<String, R> consumerRecord) {
        final InputRecord<T> record = recordMapper.transform(consumerRecord);

        final String token = Token.from(record.getTimestamp(), record.getOffset());

        return InputMessage.<T>builder()
                .withKey(record.getKey())
                .withToken(token)
                .withValue(record.getPayloadV2())
                .withTimestamp(record.getTimestamp())
                .build();
    }

    private void appendMessage(InputMessage<T> message) {
        synchronized (messages) {
            messages.add(message);
        }
    }
}