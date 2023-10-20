package com.nextbreakpoint.blueprint.common.drivers;

import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.Payload;
import com.nextbreakpoint.blueprint.common.core.RxSingleHandler;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Log4j2
public class KafkaMessagePolling<T> {
    public static final String KAFKA_POLLING_ERROR_COUNT = "kafka_polling_error_count";
    public static final String KAFKA_POLLING_QUEUE_SIZE = "kafka_polling_queue_size";
    public static final String KAFKA_POLLING_PROCESS_RECORDS_TIME = "kafka_polling_process_records_time";
    public static final String KAFKA_POLLING_CONSUME_RECORDS_TIME = "kafka_polling_consume_records_time";
    public static final String KAFKA_POLLING_SUSPEND_PARTITION = "kafka_polling_suspend_partition_count";
    public static final String KAFKA_POLLING_RESUME_PARTITION = "kafka_polling_resume_partition_count";
    public static final String KAFKA_POLLING_QUEUE_ADD_RECORD_COUNT = "kafka_polling_queue_add_record_count";
    public static final String KAFKA_POLLING_QUEUE_DELETE_RECORD_COUNT = "kafka_polling_queue_delete_record_count";

    private final Map<TopicPartition, Long> suspendedPartitions = new HashMap<>();

    private final KafkaConsumer<String, String> kafkaConsumer;

    private final Map<String, RxSingleHandler<T, ?>> messageHandlers;

    private final KafkaMessageConsumer recordsConsumer;

    private final List<Tag> tags;

    private MeterRegistry registry;

    private final KafkaRecordsQueue queue;

    private final int latency;

    private final int maxRecords;

    private long timestamp;

    private Thread pollingThread;

    public KafkaMessagePolling(KafkaConsumer<String, String> kafkaConsumer, Map<String, RxSingleHandler<T, ?>> messageHandlers, KafkaMessageConsumer recordsConsumer, MeterRegistry registry) {
        this(kafkaConsumer, messageHandlers, recordsConsumer, registry, KafkaRecordsQueue.Simple.create(), -1, 10);
    }

    public KafkaMessagePolling(KafkaConsumer<String, String> kafkaConsumer, Map<String, RxSingleHandler<T, ?>> messageHandlers, KafkaMessageConsumer recordsConsumer, MeterRegistry registry, KafkaRecordsQueue queue, int latency, int maxRecords) {
        this.kafkaConsumer = Objects.requireNonNull(kafkaConsumer);
        this.messageHandlers = Objects.requireNonNull(messageHandlers);
        this.recordsConsumer = Objects.requireNonNull(recordsConsumer);
        this.registry = Objects.requireNonNull(registry);
        this.queue = Objects.requireNonNull(queue);
        this.latency = latency;
        this.maxRecords = maxRecords;
        this.tags = List.of(Tag.of("group_id", kafkaConsumer.groupMetadata().groupId()));
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

                        registry.timer(KAFKA_POLLING_PROCESS_RECORDS_TIME, tags)
                                .record(this::processRecords);

                        Thread.yield();
                    } else {
                        registry.timer(KAFKA_POLLING_PROCESS_RECORDS_TIME, tags)
                                .record(this::processRecords);

                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }
                    }

                    resumePartitions();
                } catch (Exception e) {
                    final List<Tag> tags = List.of(
                            Tag.of("group_id", kafkaConsumer.groupMetadata().groupId()),
                            Tag.of("error", "process_records")
                    );

                    registry.counter(KAFKA_POLLING_ERROR_COUNT, tags).increment();

                    log.error("Error occurred while consuming messages", e);

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
                log.warn("Can't stop polling thread", e);
            }

            pollingThread = null;
        }
    }

    private void enqueueRecords() {
        partitionRecords(pollRecords()).forEach((topicPartition, records) -> {
            try {
                enqueueRecords(topicPartition, records);
            } catch (RecordProcessingException e) {
                suspendPartition(topicPartition, e.getRecord().offset());
            }
        });
    }

    private void enqueueRecords(TopicPartition topicPartition, List<ConsumerRecord<String, String>> records) {
        records.forEach(record -> {
            try {
                if (record.value() == null) {
                    log.trace("Skipping tombstone record {} in partition ({})", record.key(), topicPartition);

                    registry.counter(KAFKA_POLLING_QUEUE_DELETE_RECORD_COUNT, tags).increment();

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

                registry.counter(KAFKA_POLLING_QUEUE_ADD_RECORD_COUNT, tags).increment();

                queue.addRecord(new KafkaRecordsQueue.QueuedRecord(record, payload));
            } catch (Exception e) {
                final List<Tag> tags = List.of(
                        Tag.of("group_id", kafkaConsumer.groupMetadata().groupId()),
                        Tag.of("error", "enqueue_records")
                );

                registry.counter(KAFKA_POLLING_ERROR_COUNT, tags).increment();

                log.error("Failed to process record: " + record.key());

                throw new RecordProcessingException(record);
            }
        });
    }

    private void processRecords() {
        registry.summary(KAFKA_POLLING_QUEUE_SIZE, tags).record(queue.size());

        if (queue.size() > 0 && System.currentTimeMillis() - timestamp > latency) {
            log.trace("Received {} message{}", queue.size(), queue.size() > 0 ? "s" : "");

            partitionQueuedRecords(queue.getRecords()).forEach((topicPartition, records) -> {
                try {
                    consumeRecords(topicPartition, records);
                } catch (RecordProcessingException e) {
                    final List<Tag> tags = List.of(
                            Tag.of("group_id", kafkaConsumer.groupMetadata().groupId()),
                            Tag.of("error", "consume_records")
                    );

                    registry.counter(KAFKA_POLLING_ERROR_COUNT, tags).increment();

                    suspendPartition(topicPartition, e.getRecord().offset());
                }
            });

            queue.clear();

            commitOffsets();
        }
    }

    private void consumeRecords(TopicPartition topicPartition, List<KafkaRecordsQueue.QueuedRecord> records) {
        try {
            registry.timer(KAFKA_POLLING_CONSUME_RECORDS_TIME, tags)
                    .record(() -> recordsConsumer.consumeRecords(topicPartition, records));
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

    private void resumePartitions() {
        long timestamp = System.currentTimeMillis();

        List<TopicPartition> resumePartitions = suspendedPartitions.entrySet().stream()
                .filter(entry -> timestamp - entry.getValue() > 5000)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        resumePartitions(resumePartitions);
    }

    private ConsumerRecords<String, String> pollRecords() {
        return kafkaConsumer.poll(Duration.ofSeconds(5));
    }

    private void commitOffsets() {
        kafkaConsumer.commitSync();
    }

    private void suspendPartition(TopicPartition topicPartition, long offset) {
        kafkaConsumer.pause(List.of(topicPartition));
        kafkaConsumer.seek(topicPartition, offset);
        suspendedPartitions.put(topicPartition, System.currentTimeMillis());
        registry.counter(KAFKA_POLLING_SUSPEND_PARTITION, tags).increment();
    }

    private void resumePartitions(List<TopicPartition> topicPartitions) {
        if (topicPartitions.size() > 0) {
            kafkaConsumer.resume(topicPartitions);
            topicPartitions.forEach(suspendedPartitions::remove);
            registry.counter(KAFKA_POLLING_RESUME_PARTITION, tags).increment(suspendedPartitions.size());
        }
    }

    private Map<TopicPartition, List<ConsumerRecord<String, String>>> partitionRecords(ConsumerRecords<String, String> records) {
        final Map<TopicPartition, List<ConsumerRecord<String, String>>> partitionedRecords = new HashMap<>();

        records.forEach(record -> {
            final TopicPartition topicPartition = new TopicPartition(record.topic(), record.partition());

            final List<ConsumerRecord<String, String>> recordList = partitionedRecords.computeIfAbsent(topicPartition, key -> new ArrayList<>());

            recordList.add(record);
        });

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
        private ConsumerRecord<String, String> record;

        public RecordProcessingException(ConsumerRecord<String, String> record) {
            this.record = record;
        }

        public ConsumerRecord<String, String> getRecord() {
            return record;
        }
    }
}
