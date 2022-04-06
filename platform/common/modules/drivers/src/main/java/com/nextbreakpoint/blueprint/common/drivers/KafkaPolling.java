package com.nextbreakpoint.blueprint.common.drivers;

import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.Payload;
import com.nextbreakpoint.blueprint.common.core.RxSingleHandler;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Log4j2
public class KafkaPolling<T> {
    private final Map<TopicPartition, Long> suspendedPartitions = new HashMap<>();

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

                    resumePartitions();
                } catch (Exception e) {
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
                    log.debug("Skipping tombstone record " + record.key() + " in partition (" + topicPartition + ")");

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
                log.error("Failed to process record: " + record.key());

                throw new RecordProcessingException(record);
            }
        });
    }

    private void processRecords() {
        if (queue.size() > 0 && System.currentTimeMillis() - timestamp > latency) {
            log.debug("Received " + queue.size() + " " + (queue.size() > 0 ? "messages" : "message"));

            partitionQueuedRecords(queue.getRecords()).forEach((topicPartition, records) -> {
                try {
                    consumeRecords(topicPartition, records);
                } catch (RecordProcessingException e) {
                    suspendPartition(topicPartition, e.getRecord().offset());
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
    }

    private void resumePartitions(List<TopicPartition> topicPartitions) {
        if (topicPartitions.size() > 0) {
            kafkaConsumer.resume(topicPartitions);
            topicPartitions.forEach(suspendedPartitions::remove);
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
