package com.nextbreakpoint.blueprint.common.vertx;

import com.nextbreakpoint.blueprint.common.core.*;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.kafka.client.common.TopicPartition;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumer;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumerRecords;
import io.vertx.rxjava.kafka.client.producer.KafkaHeader;
import rx.schedulers.Schedulers;

import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class KafkaPolling {
    private static final Logger logger = LoggerFactory.getLogger(KafkaPolling.class.getName());

    private final KafkaConsumer<String, String> kafkaConsumer;

    private final Map<String, BlockingHandler<InputMessage>> messageHandlers;

    private final KafkaRecordsQueue queue;

    private final int latency;

    private final int maxRecords;

    private final Set<TopicPartition> suspendedPartitions = new HashSet<>();

    private long timestamp;

    private Thread pollingThread;

    public KafkaPolling(KafkaConsumer<String, String> kafkaConsumer, Map<String, BlockingHandler<InputMessage>> messageHandlers) {
        this(kafkaConsumer, messageHandlers, KafkaRecordsQueue.Simple.create(), -1, 10);
    }

    public KafkaPolling(KafkaConsumer<String, String> kafkaConsumer, Map<String, BlockingHandler<InputMessage>> messageHandlers, KafkaRecordsQueue queue, int latency, int maxRecords) {
        this.kafkaConsumer = Objects.requireNonNull(kafkaConsumer);
        this.messageHandlers = Objects.requireNonNull(messageHandlers);
        this.queue = Objects.requireNonNull(queue);
        this.latency = latency;
        this.maxRecords = maxRecords;
    }

    public void startPolling(String name) {
        if (pollingThread != null) {
            return;
        }

        pollingThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    if (queue.size() < maxRecords) {
                        enqueueRecords(pollRecords());

                        processRecords();

                        Thread.yield();
                    } else {
                        processRecords();

                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error occurred while consuming messages", e);

                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }, name);

        pollingThread.start();
    }

    public void stopPolling() {
        if (pollingThread != null) {
            try {
                pollingThread.interrupt();
                pollingThread.join();
            } catch (InterruptedException e) {
                logger.warn("Can't stop polling thread", e);
            }

            pollingThread = null;
        }
    }

    private void enqueueRecords(KafkaConsumerRecords<String, String> records) {
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

                    queue.deleteRecord(record);

                    continue;
                }

                final Payload payload = Json.decodeValue(record.value(), Payload.class);

                final BlockingHandler<InputMessage> handler = messageHandlers.get(payload.getType());

                if (handler == null) {
                    continue;
                }

                if (queue.size() == 0) {
                    timestamp = System.currentTimeMillis();
                }

                queue.addRecord(record);
            } catch (Exception e) {
                logger.error("Failed to process record: " + record.key());

                suspendedPartitions.add(topicPartition);

                retryPartition(record, topicPartition);
            }
        }

        suspendedPartitions.clear();
    }

    private void processRecords() {
        final long currentTimeMillis = System.currentTimeMillis();

        if (queue.size() > 0 && currentTimeMillis - timestamp > latency) {
            logger.debug("Received " + queue.size() + " " + (queue.size() > 0 ? "messages" : "message"));

            queue.getRecords().forEach(this::processRecord);

            queue.clear();

            commitOffsets();
        }
    }

    private void processRecord(KafkaConsumerRecord<String, String> record) {
        final TopicPartition topicPartition = new TopicPartition(record.topic(), record.partition());

        try {
            if (suspendedPartitions.contains(topicPartition)) {
                logger.debug("Skipping record " + record.key() + " from suspended partition (" + topicPartition + ")");

                return;
            }

            Map<String, String> headers = record.headers().stream()
                    .collect(Collectors.toMap(KafkaHeader::key, kafkaHeader -> getString(kafkaHeader.value())));

            final Payload payload = Json.decodeValue(record.value(), Payload.class);

            final BlockingHandler<InputMessage> handler = messageHandlers.get(payload.getType());

            if (handler == null) {
                return;
            }

            final String token = Token.from(record.timestamp(), record.offset());

            final InputMessage message = new InputMessage(record.key(), token, payload, Tracing.from(headers), record.timestamp());

            logger.debug("Received message: " + message);

            handler.handleBlocking(message);
        } catch (Exception e) {
            logger.error("Failed to process record: " + record.key());

            suspendedPartitions.add(topicPartition);

            retryPartition(record, topicPartition);
        }

        suspendedPartitions.clear();
    }

    private String getString(Buffer value) {
        return value != null ? value.toString() : null;
    }

    private KafkaConsumerRecords<String, String> pollRecords() {
        return kafkaConsumer.fetch(Math.min(10, maxRecords))
                .rxPoll(Duration.ofSeconds(10))
                .subscribeOn(Schedulers.computation())
                .doOnError(err -> logger.error("Failed to consume records", err))
                .toBlocking()
                .value();
    }

    private void commitOffsets() {
        kafkaConsumer.rxCommit()
                .subscribeOn(Schedulers.computation())
                .doOnError(err -> logger.error("Failed to commit offsets", err))
                .toCompletable()
                .await();
    }

    private void retryPartition(KafkaConsumerRecord<String, String> record, TopicPartition topicPartition) {
        kafkaConsumer.rxPause(topicPartition)
                .subscribeOn(Schedulers.computation())
                .flatMap(x -> kafkaConsumer.rxSeek(topicPartition, record.offset()))
                .delay(10, TimeUnit.SECONDS)
                .flatMap(x -> kafkaConsumer.rxResume(topicPartition))
                .doOnError(err -> logger.error("Failed to resume partition (" + topicPartition + ")", err))
                .toCompletable()
                .await();
    }
}
