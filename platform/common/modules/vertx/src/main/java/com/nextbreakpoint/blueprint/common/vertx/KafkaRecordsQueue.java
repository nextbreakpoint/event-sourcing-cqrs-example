package com.nextbreakpoint.blueprint.common.vertx;

import io.vertx.rxjava.kafka.client.consumer.KafkaConsumerRecord;

import java.util.*;

public interface KafkaRecordsQueue {
    Simple Simple = new KafkaRecordsQueue.Simple();
    Compacted Compacted = new KafkaRecordsQueue.Compacted();

    void addRecord(KafkaConsumerRecord<String, String> record);

    Collection<KafkaConsumerRecord<String, String>> getRecords();

    int size();

    void clear();

    class Simple implements KafkaRecordsQueue {
        private List<KafkaConsumerRecord<String, String>> records = new ArrayList<>();

        @Override
        public void addRecord(KafkaConsumerRecord<String, String> record) {
            records.add(record);
        }

        @Override
        public Collection<KafkaConsumerRecord<String, String>> getRecords() {
            return new ArrayList<>(records);
        }

        @Override
        public int size() {
            return records.size();
        }

        @Override
        public void clear() {
            records.clear();
        }
    }

    class Compacted implements KafkaRecordsQueue {
        private Map<String, KafkaConsumerRecord<String, String>> records = new HashMap<>();

        @Override
        public void addRecord(KafkaConsumerRecord<String, String> record) {
            records.put(record.key(), record);
        }

        @Override
        public Collection<KafkaConsumerRecord<String, String>> getRecords() {
            return records.values();
        }

        @Override
        public int size() {
            return records.size();
        }

        @Override
        public void clear() {
            records.clear();
        }
    }
}
