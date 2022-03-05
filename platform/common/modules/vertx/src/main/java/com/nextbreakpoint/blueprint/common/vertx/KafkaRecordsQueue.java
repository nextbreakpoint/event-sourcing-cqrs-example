package com.nextbreakpoint.blueprint.common.vertx;

import io.vertx.rxjava.kafka.client.consumer.KafkaConsumerRecord;

import java.util.*;

public interface KafkaRecordsQueue {
    void addRecord(KafkaConsumerRecord<String, String> record);

    Collection<KafkaConsumerRecord<String, String>> getRecords();

    void deleteRecord(KafkaConsumerRecord<String, String> record);

    int size();

    void clear();

    class Simple implements KafkaRecordsQueue {
        private List<KafkaConsumerRecord<String, String>> records = new ArrayList<>();

        @Override
        public void addRecord(KafkaConsumerRecord<String, String> record) {
            records.add(record);
        }

        @Override
        public void deleteRecord(KafkaConsumerRecord<String, String> record) {}

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

        public static Simple create() {
            return new Simple();
        }
    }

    class Compacted implements KafkaRecordsQueue {
        private Map<String, KafkaConsumerRecord<String, String>> records = new HashMap<>();

        @Override
        public void addRecord(KafkaConsumerRecord<String, String> record) {
            records.put(record.key(), record);
        }

        @Override
        public void deleteRecord(KafkaConsumerRecord<String, String> record) {
            records.remove(record.key());
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

        public static Compacted create() {
            return new Compacted();
        }
    }
}
