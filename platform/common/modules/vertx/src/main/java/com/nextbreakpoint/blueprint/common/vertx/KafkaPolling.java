package com.nextbreakpoint.blueprint.common.vertx;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Payload;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.Json;
import io.vertx.kafka.client.common.TopicPartition;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumer;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumerRecords;
import rx.schedulers.Schedulers;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class KafkaPolling {
    private static final Logger logger = LoggerFactory.getLogger(KafkaPolling.class.getName());

    private int pollingInterval = 30000;

    public void pollRecords(KafkaConsumer<String, String> kafkaConsumer, Map<String, MessageHandler<InputMessage, Void>> messageHandlers) {
        for (;;) {
            try {
                processRecords(kafkaConsumer, messageHandlers);

                Thread.yield();
            } catch (Exception e) {
                logger.error("Error occurred while consuming messages", e);

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ex) {
                }
            }
        }
    }

    public void pollRecordsWithCompaction(KafkaConsumer<String, String> kafkaConsumer, Map<String, MessageHandler<InputMessage, Void>> messageHandlers) {
        for (;;) {
            try {
                processRecordsWithCompaction(kafkaConsumer, messageHandlers);

                Thread.yield();
            } catch (Exception e) {
                logger.error("Error occurred while consuming messages", e);

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ex) {
                }
            }
        }
    }

    private void processRecords(KafkaConsumer<String, String> kafkaConsumer, Map<String, MessageHandler<InputMessage, Void>> messageHandlers) {
        final List<KafkaConsumerRecord<String, String>> buffer = new ArrayList<>();

        final KafkaConsumerRecords<String, String> records = pollRecords(kafkaConsumer);

        for (int i = 0; i < records.size(); i++) {
            final KafkaConsumerRecord<String, String> record = records.recordAt(i);

            buffer.add(record);
        }

        final Set<TopicPartition> suspendedPartitions = new HashSet<>();

        buffer.forEach(record -> processRecord(kafkaConsumer, messageHandlers, suspendedPartitions, record));

        commitOffsets(kafkaConsumer);
    }

    private void processRecordsWithCompaction(KafkaConsumer<String, String> kafkaConsumer, Map<String, MessageHandler<InputMessage, Void>> messageHandlers) {
        final Set<TopicPartition> suspendedPartitions = new HashSet<>();

        final Map<String, KafkaConsumerRecord<String, String>> buffer = new HashMap<>();

        long timestamp = System.currentTimeMillis();

        for (;;) {
            final KafkaConsumerRecords<String, String> records = pollRecords(kafkaConsumer);

            for (int i = 0; i < records.size(); i++) {
                final KafkaConsumerRecord<String, String> record = records.recordAt(i);

                final TopicPartition topicPartition = new TopicPartition(record.topic(), record.partition());

                try {
                    if (suspendedPartitions.contains(topicPartition)) {
                        logger.debug("Skipping record " + record.key() + " from suspended partition (" + topicPartition + ")");

                        continue;
                    }

                    if (record.value() == null) {
                        logger.debug("Skipping tombstone record " + record.key() + " from partition (" + topicPartition + ")");

                        continue;
                    }

                    final Payload payload = Json.decodeValue(record.value(), Payload.class);

                    final InputMessage message = new InputMessage(record.key(), record.offset(), payload, record.timestamp());

                    final MessageHandler<InputMessage, Void> handler = messageHandlers.get(payload.getType());

                    if (handler == null) {
//                        logger.warn("Ignoring message of type: " + message.getType());

                        continue;
                    }

                    buffer.put(message.getKey(), record);
                } catch (Exception e) {
                    logger.error("Failed to process record: " + record.key());

                    suspendedPartitions.add(topicPartition);

                    retryPartition(kafkaConsumer, record, topicPartition);
                }
            }

            final long duration = System.currentTimeMillis() - timestamp;

            if (duration > pollingInterval) {
                break;
            }
        }

        logger.info("Total compacted messages: " + buffer.size());

        buffer.values().forEach(record -> processRecord(kafkaConsumer, messageHandlers, suspendedPartitions, record));

        commitOffsets(kafkaConsumer);
    }

    private void processRecord(KafkaConsumer<String, String> kafkaConsumer, Map<String, MessageHandler<InputMessage, Void>> messageHandlers, Set<TopicPartition> suspendedPartitions, KafkaConsumerRecord<String, String> record) {
        final TopicPartition topicPartition = new TopicPartition(record.topic(), record.partition());

        try {
            if (suspendedPartitions.contains(topicPartition)) {
                logger.debug("Skipping record " + record.key() + " from suspended partition (" + topicPartition + ")");

                return;
            }

            if (record.value() == null) {
                logger.debug("Skipping tombstone record " + record.key() + " from partition (" + topicPartition + ")");

                return;
            }

            final Payload payload = Json.decodeValue(record.value(), Payload.class);

            final InputMessage message = new InputMessage(record.key(), record.offset(), payload, record.timestamp());

            final MessageHandler<InputMessage, Void> handler = messageHandlers.get(payload.getType());

            if (handler == null) {
//                logger.warn("Ignoring message of type: " + message.getType());

                return;
            }

//            logger.debug("Received message: " + message);

            handler.handleBlocking(message);
        } catch (Exception e) {
            logger.error("Failed to process record: " + record.key());

            suspendedPartitions.add(topicPartition);

            retryPartition(kafkaConsumer, record, topicPartition);
        }
    }

    private KafkaConsumerRecords<String, String> pollRecords(KafkaConsumer<String, String> kafkaConsumer) {
        return kafkaConsumer.rxPoll(Duration.ofSeconds(10))
                .subscribeOn(Schedulers.computation())
                .doOnError(err -> logger.error("Failed to consume records", err))
                .toBlocking()
                .value();
    }

    private void commitOffsets(KafkaConsumer<String, String> kafkaConsumer) {
        kafkaConsumer.rxCommit()
                .subscribeOn(Schedulers.computation())
                .doOnError(err -> logger.error("Failed to commit offsets", err))
                .toCompletable()
                .await();
    }

    private void retryPartition(KafkaConsumer<String, String> kafkaConsumer, KafkaConsumerRecord<String, String> record, TopicPartition topicPartition) {
        kafkaConsumer.rxPause(topicPartition)
                .subscribeOn(Schedulers.computation())
                .flatMap(x -> kafkaConsumer.rxSeek(topicPartition, record.offset()))
                .delay(5, TimeUnit.SECONDS)
                .flatMap(x -> kafkaConsumer.rxResume(topicPartition))
                .doOnError(err -> logger.error("Failed to resume partition (" + topicPartition + ")", err))
                .toCompletable()
                .await();
    }
}
