package com.nextbreakpoint.blueprint.common.vertx;

import com.nextbreakpoint.blueprint.common.core.*;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.kafka.client.common.TopicPartition;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumer;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumerRecords;
import rx.schedulers.Schedulers;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class KafkaPolling<T> {
    private static final Logger logger = LoggerFactory.getLogger(KafkaPolling.class.getName());

    private final KafkaConsumer<String, String> kafkaConsumer;

    private final Map<String, RxSingleHandler<T, ?>> messageHandlers;

    private final KafkaRecordsConsumer recordsConsumer;

    private final KafkaRecordsQueue queue;

    private final int latency;

    private final int maxRecords;

    private long timestamp;

    private Thread pollingThread;

    public KafkaPolling(KafkaConsumer<String, String> kafkaConsumer, Map<String, RxSingleHandler<T, ?>> messageHandlers, KafkaRecordsConsumer recordsConsumer) {
        this(kafkaConsumer, messageHandlers, recordsConsumer, KafkaRecordsQueue.Simple.create(), -1, 10);
    }

    public KafkaPolling(KafkaConsumer<String, String> kafkaConsumer, Map<String, RxSingleHandler<T, ?>> messageHandlers, KafkaRecordsConsumer recordsConsumer, KafkaRecordsQueue queue, int latency, int maxRecords) {
        this.kafkaConsumer = Objects.requireNonNull(kafkaConsumer);
        this.messageHandlers = Objects.requireNonNull(messageHandlers);
        this.recordsConsumer = Objects.requireNonNull(recordsConsumer);
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
                        enqueueRecords();

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

    private void enqueueRecords() {
        partitionRecords(pollRecords()).forEach((topicPartition, records) -> {
            try {
                enqueueRecords(topicPartition, records);
            } catch (RecordProcessingException e) {
                retryPartition(topicPartition, e.getRecord().offset());
            }
        });
    }

    private void enqueueRecords(TopicPartition topicPartition, List<KafkaConsumerRecord<String, String>> records) {
        records.forEach(record -> {
            try {
                if (record.value() == null) {
                    logger.debug("Skipping tombstone record " + record.key() + " in partition (" + topicPartition + ")");

                    queue.deleteRecord(new KafkaRecordsQueue.QueuedRecord(record, null));

                    return;
                }

                final Payload payload = Json.decodeValue(record.value(), Payload.class);

                final RxSingleHandler<T, ?> handler = messageHandlers.get(payload.getType());

                if (handler == null) {
                    return;
                }

                if (queue.size() == 0) {
                    timestamp = System.currentTimeMillis();
                }

                queue.addRecord(new KafkaRecordsQueue.QueuedRecord(record, payload));
            } catch (Exception e) {
                logger.error("Failed to process record: " + record.key());

                throw new RecordProcessingException(record);
            }
        });
    }

    private void processRecords() {
        if (queue.size() > 0 && System.currentTimeMillis() - timestamp > latency) {
            logger.debug("Received " + queue.size() + " " + (queue.size() > 0 ? "messages" : "message"));

            partitionQueuedRecords(queue.getRecords()).forEach((topicPartition, records) -> {
                try {
                    consumeRecords(topicPartition, records);
                } catch (RecordProcessingException e) {
                    retryPartition(topicPartition, e.getRecord().offset());
                }
            });

            queue.clear();

            commitOffsets();
        }
    }

    private void consumeRecords(TopicPartition topicPartition, List<KafkaRecordsQueue.QueuedRecord> records) {
        try {
            recordsConsumer.consumeRecords(topicPartition, records);
        } catch (RecordProcessingException e) {
            if (queue instanceof KafkaRecordsQueue.Compacted) {
                throw new RecordProcessingException(records.get(0).getRecord());
            } else {
                throw e;
            }
        } catch (Exception e) {
            throw new RecordProcessingException(records.get(0).getRecord());
        }
    }

    private KafkaConsumerRecords<String, String> pollRecords() {
        return kafkaConsumer.fetch(Math.min(25, maxRecords))
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

    private void retryPartition(TopicPartition topicPartition, long offset) {
        kafkaConsumer.rxPause(topicPartition)
                .subscribeOn(Schedulers.computation())
                .flatMap(x -> kafkaConsumer.rxSeek(topicPartition, offset))
                .delay(10, TimeUnit.SECONDS)
                .flatMap(x -> kafkaConsumer.rxResume(topicPartition))
                .doOnError(err -> logger.error("Failed to resume partition (" + topicPartition + ")", err))
                .toCompletable()
                .await();
    }

    private Map<TopicPartition, List<KafkaConsumerRecord<String, String>>> partitionRecords(KafkaConsumerRecords<String, String> records) {
        final Map<TopicPartition, List<KafkaConsumerRecord<String, String>>> partitionedRecords = new HashMap<>();

        for (int i = 0; i < records.size(); i++) {
            final KafkaConsumerRecord<String, String> record = records.recordAt(i);

            final TopicPartition topicPartition = new TopicPartition(record.topic(), record.partition());

            final List<KafkaConsumerRecord<String, String>> recordList = partitionedRecords.computeIfAbsent(topicPartition, key -> new ArrayList<>());

            recordList.add(record);
        }

        return partitionedRecords;
    }

    private Map<TopicPartition, List<KafkaRecordsQueue.QueuedRecord>> partitionQueuedRecords(List<KafkaRecordsQueue.QueuedRecord> records) {
        final Map<TopicPartition, List<KafkaRecordsQueue.QueuedRecord>> partitionedRecords = new HashMap<>();

        records.forEach(record -> {
            final TopicPartition topicPartition = new TopicPartition(record.getRecord().topic(), record.getRecord().partition());

            final List<KafkaRecordsQueue.QueuedRecord> recordList = partitionedRecords.computeIfAbsent(topicPartition, key -> new ArrayList<>());

            recordList.add(record);
        });

        return partitionedRecords;
    }

    public static class RecordProcessingException extends RuntimeException {
        private KafkaConsumerRecord<String, String> record;

        public RecordProcessingException(KafkaConsumerRecord<String, String> record) {
            this.record = record;
        }

        public KafkaConsumerRecord<String, String> getRecord() {
            return record;
        }
    }
}
