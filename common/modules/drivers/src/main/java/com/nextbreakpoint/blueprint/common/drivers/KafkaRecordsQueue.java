package com.nextbreakpoint.blueprint.common.drivers;

import com.nextbreakpoint.blueprint.common.core.Payload;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface KafkaRecordsQueue {
    void addRecord(QueuedRecord entry);

    List<QueuedRecord> getRecords();

    void deleteRecord(QueuedRecord entry);

    int size();

    void clear();

    class Simple implements KafkaRecordsQueue {
        private List<QueuedRecord> records = new ArrayList<>();

        @Override
        public void addRecord(QueuedRecord record) {
            records.add(record);
        }

        @Override
        public void deleteRecord(QueuedRecord record) {}

        @Override
        public List<QueuedRecord> getRecords() {
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
        private Map<String, QueuedRecord> records = new HashMap<>();

        @Override
        public void addRecord(QueuedRecord record) {
            records.put(record.record.key(), record);
        }

        @Override
        public void deleteRecord(QueuedRecord record) {
            records.remove(record.record.key());
        }

        @Override
        public List<QueuedRecord> getRecords() {
            return new ArrayList<>(records.values());
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

    class QueuedRecord {
        private ConsumerRecord<String, String> record;
        private Payload payload;

        public QueuedRecord(ConsumerRecord<String, String> record, Payload payload) {
            this.record = record;
            this.payload = payload;
        }

        public ConsumerRecord<String, String> getRecord() {
            return record;
        }

        public Payload getPayload() {
            return payload;
        }
    }
}
