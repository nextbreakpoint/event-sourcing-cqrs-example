package com.nextbreakpoint.blueprint.common.drivers;

import com.nextbreakpoint.blueprint.common.core.InputRecord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface KafkaRecordsQueue<T> {
    void addRecord(QueuedRecord<T> entry);

    List<QueuedRecord<T>> getRecords();

    void deleteRecord(QueuedRecord<T> entry);

    int size();

    void clear();

    class Simple<T> implements KafkaRecordsQueue<T> {
        private List<QueuedRecord<T>> records = new ArrayList<>();

        @Override
        public void addRecord(QueuedRecord<T> record) {
            records.add(record);
        }

        @Override
        public void deleteRecord(QueuedRecord<T> record) {}

        @Override
        public List<QueuedRecord<T>> getRecords() {
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

        public static <T> Simple<T> create() {
            return new Simple<>();
        }
    }

    class Compacted<T> implements KafkaRecordsQueue<T> {
        private Map<String, QueuedRecord<T>> records = new HashMap<>();

        @Override
        public void addRecord(QueuedRecord<T> record) {
            records.put(record.record.getKey(), record);
        }

        @Override
        public void deleteRecord(QueuedRecord<T> record) {
            records.remove(record.record.getKey());
        }

        @Override
        public List<QueuedRecord<T>> getRecords() {
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

        public static <T> Compacted<T> create() {
            return new Compacted<>();
        }
    }

    class QueuedRecord<T> {
        private InputRecord<T> record;

        public QueuedRecord(InputRecord<T> record) {
            this.record = record;
        }

        public InputRecord<T> getRecord() {
            return record;
        }
    }
}
