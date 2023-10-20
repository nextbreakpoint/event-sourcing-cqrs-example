package com.nextbreakpoint.blueprint.common.drivers;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.RxSingleHandler;
import com.nextbreakpoint.blueprint.common.core.Token;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import rx.schedulers.Schedulers;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public interface KafkaMessageConsumer {
    String KAFKA_CONSUMER_ERROR_COUNT = "kafka_consumer_error_count";
    String KAFKA_CONSUMER_RECORD_TYPE_COUNT = "kafka_consumer_record_type_count";
    String KAFKA_CONSUMER_RECORD_LATENCY_SECONDS = "kafka_consumer_record_latency_seconds";

    void consumeRecords(TopicPartition topicPartition, List<KafkaRecordsQueue.QueuedRecord> records);

    @Log4j2
    class Simple implements KafkaMessageConsumer {
        private final Map<String, RxSingleHandler<InputMessage, ?>> messageHandlers;

        private final Tracer tracer;

        private MeterRegistry registry;

        private final TextMapGetter<Map<String, String>> getter = new TextMapGetter<>() {
            @Override
            public String get(Map<String, String> headers, String key) {
                return headers.get(key);
            }

            @Override
            public Iterable<String> keys(Map<String, String> headers) {
                return headers.keySet();
            }
        };

        public static KafkaMessageConsumer create(Map<String, RxSingleHandler<InputMessage, ?>> messageHandlers, MeterRegistry registry) {
            return new Simple(messageHandlers, registry);
        }

        private Simple(Map<String, RxSingleHandler<InputMessage, ?>> messageHandlers, MeterRegistry registry) {
            this.messageHandlers = Objects.requireNonNull(messageHandlers);
            this.registry = Objects.requireNonNull(registry);

            tracer = GlobalOpenTelemetry.getTracer("com.nextbreakpoint.blueprint");
        }

        @Override
        public void consumeRecords(TopicPartition topicPartition, List<KafkaRecordsQueue.QueuedRecord> records) {
            records.forEach(record -> consumeMessage(topicPartition, record));
        }

        private void consumeMessage(TopicPartition topicPartition, KafkaRecordsQueue.QueuedRecord record) {
            try {
                final RxSingleHandler<InputMessage, ?> handler = messageHandlers.get(record.getPayload().getType());

                if (handler == null) {
                    return;
                }

                final String token = Token.from(record.getRecord().timestamp(), record.getRecord().offset());

                final InputMessage message = new InputMessage(record.getRecord().key(), token, record.getPayload(), record.getRecord().timestamp());

                final Map<String, String> headers = StreamSupport.stream(record.getRecord().headers().spliterator(), false)
                        .collect(Collectors.toMap(Header::key, kafkaHeader -> getString(kafkaHeader.value())));

                final Context extractedContext = W3CTraceContextPropagator.getInstance().extract(Context.current(), headers, getter);

                final Span messageSpan = tracer.spanBuilder("Received message " + message.getValue().getType()).setParent(extractedContext).startSpan();

                try (Scope scope = messageSpan.makeCurrent()) {
                    final Span span = Span.current();

                    span.setAttribute("message.source", message.getValue().getSource());
                    span.setAttribute("message.type", message.getValue().getType());
                    span.setAttribute("message.uuid", message.getValue().getUuid().toString());
                    span.setAttribute("message.key", record.getRecord().key());
                    span.setAttribute("message.token", token);
                    span.setAttribute("message.topic", record.getRecord().topic());
                    span.setAttribute("message.offset", record.getRecord().offset());
                    span.setAttribute("message.timestamp", record.getRecord().timestamp());

                    log.trace("Received message from topic {}: {}", topicPartition.topic(), message);

                    final List<Tag> tags = List.of(
                            Tag.of("topic", topicPartition.topic()),
                            Tag.of("type", message.getValue().getType())
                    );

                    registry.counter(KAFKA_CONSUMER_RECORD_TYPE_COUNT, tags).increment();

                    registry.summary(KAFKA_CONSUMER_RECORD_LATENCY_SECONDS, tags)
                            .record((System.currentTimeMillis() - message.getTimestamp()) / 1000.0);

                    handler.handleSingle(message)
                            .subscribeOn(Schedulers.immediate())
                            .toCompletable()
                            .await();
                } finally {
                    messageSpan.end();
                }
            } catch (Exception e) {
                final List<Tag> tags = List.of(
                        Tag.of("topic", topicPartition.topic())
                );

                registry.counter(KAFKA_CONSUMER_ERROR_COUNT, tags).increment();

                log.error("Failed to consume 1 record: {}", record.getRecord().key());

                throw new KafkaMessagePolling.RecordProcessingException(record.getRecord());
            }
        }

        private String getString(byte[] value) {
            return value != null ? new String(value) : null;
        }
    }

    @Log4j2
    class Buffered implements KafkaMessageConsumer {
        private final Map<String, RxSingleHandler<List<InputMessage>, ?>> messageHandlers;

        private final Tracer tracer;

        private final MeterRegistry registry;

        private final TextMapGetter<Map<String, String>> getter = new TextMapGetter<>() {
            @Override
            public String get(Map<String, String> headers, String key) {
                return headers.get(key);
            }

            @Override
            public Iterable<String> keys(Map<String, String> headers) {
                return headers.keySet();
            }
        };

        public static KafkaMessageConsumer create(Map<String, RxSingleHandler<List<InputMessage>, ?>> messageHandlers, MeterRegistry registry) {
            return new Buffered(messageHandlers, registry);
        }

        public Buffered(Map<String, RxSingleHandler<List<InputMessage>, ?>> messageHandlers, MeterRegistry registry) {
            this.messageHandlers = Objects.requireNonNull(messageHandlers);
            this.registry = Objects.requireNonNull(registry);

            tracer = GlobalOpenTelemetry.getTracer("com.nextbreakpoint.blueprint");
        }

        @Override
        public void consumeRecords(TopicPartition topicPartition, List<KafkaRecordsQueue.QueuedRecord> records) {
            try {
                records.stream().map(this::convertRecord)
                        .collect(Collectors.groupingBy(InputMessage::getKey))
                        .values()
                        .forEach(groupedMessages -> consumeMessage(topicPartition, records, groupedMessages));
            } catch (Exception e) {
                final List<Tag> tags = List.of(
                        Tag.of("topic", topicPartition.topic())
                );

                registry.counter(KAFKA_CONSUMER_ERROR_COUNT, tags).increment();

                log.error("Failed to consume {} records", records.size());

                throw new KafkaMessagePolling.RecordProcessingException(records.get(0).getRecord());
            }
        }

        private InputMessage convertRecord(KafkaRecordsQueue.QueuedRecord record) {
            final String token = Token.from(record.getRecord().timestamp(), record.getRecord().offset());

            return new InputMessage(record.getRecord().key(), token, record.getPayload(), record.getRecord().timestamp());
        }

        private void consumeMessage(TopicPartition topicPartition, List<KafkaRecordsQueue.QueuedRecord> records, List<InputMessage> groupedMessages) {
            final Map<String, List<InputMessage>> messagesByType = groupedMessages.stream()
                    .peek(message -> log.trace("Received message from topic {}: {}", topicPartition.topic(), message))
                    .collect(Collectors.groupingBy(message -> message.getValue().getType()));

            messagesByType.keySet().forEach(type -> {
                final List<InputMessage> messages = messagesByType.get(type);

                if (messages.isEmpty()) {
                    return;
                }

                final RxSingleHandler<List<InputMessage>, ?> handler = messageHandlers.get(type);

                if (handler == null) {
                    return;
                }

                final Map<String, String> headers = StreamSupport.stream(records.get(0).getRecord().headers().spliterator(), false)
                        .collect(Collectors.toMap(Header::key, kafkaHeader -> getString(kafkaHeader.value())));

                final Context extractedContext = W3CTraceContextPropagator.getInstance().extract(Context.current(), headers, getter);

                final Span messageSpan = tracer.spanBuilder("Received message " + type).setParent(extractedContext).startSpan();

                try (Scope scope = messageSpan.makeCurrent()) {
                    final Span span = Span.current();

                    span.setAttribute("message.type", type);
                    span.setAttribute("message.count", messages.size());

                    final List<Tag> tags = List.of(
                            Tag.of("topic", topicPartition.topic()),
                            Tag.of("type", type)
                    );

                    registry.counter(KAFKA_CONSUMER_RECORD_TYPE_COUNT, tags).increment(messages.size());

                    registry.summary(KAFKA_CONSUMER_RECORD_LATENCY_SECONDS, tags)
                            .record((System.currentTimeMillis() - messages.get(0).getTimestamp()) / 1000.0);

                    handler.handleSingle(messages)
                            .subscribeOn(Schedulers.immediate())
                            .toCompletable()
                            .await();
                } finally {
                    messageSpan.end();
                }
            });
        }

        private String getString(byte[] value) {
            return value != null ? new String(value) : null;
        }
    }

    class MessageAndRecord {
        private KafkaRecordsQueue.QueuedRecord record;
        private InputMessage message;

        public MessageAndRecord(KafkaRecordsQueue.QueuedRecord record, InputMessage message) {
            this.record = record;
            this.message = message;
        }

        public KafkaRecordsQueue.QueuedRecord getRecord() {
            return record;
        }

        public InputMessage getMessage() {
            return message;
        }
    }
}
