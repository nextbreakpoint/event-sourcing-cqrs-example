package com.nextbreakpoint.blueprint.common.drivers;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Payload;
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
    String VERTX_KAFKA_CONSUMER_ERROR_COUNT = "vertx_kafka_consumer_error_count";
    String VERTX_KAFKA_CONSUMER_RECORD_TYPE_COUNT = "vertx_kafka_consumer_record_type_count";
    String VERTX_KAFKA_CONSUMER_RECORD_LAG_SECONDS = "vertx_kafka_consumer_record_lag_seconds";

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
                final Payload payload = record.getPayload();

                final Map<String, String> headers = StreamSupport.stream(record.getRecord().headers().spliterator(), false)
                        .collect(Collectors.toMap(Header::key, kafkaHeader -> getString(kafkaHeader.value())));

                final Context extractedContext = W3CTraceContextPropagator.getInstance().extract(Context.current(), headers, getter);

                final Span messageSpan = tracer.spanBuilder("Received message " + payload.getType()).setParent(extractedContext).startSpan();

                try (Scope scope = messageSpan.makeCurrent()) {
                    final Span span = Span.current();

                    final String token = Token.from(record.getRecord().timestamp(), record.getRecord().offset());

                    span.setAttribute("message.source", payload.getSource());
                    span.setAttribute("message.type", payload.getType());
                    span.setAttribute("message.uuid", payload.getUuid().toString());
                    span.setAttribute("message.key", record.getRecord().key());
                    span.setAttribute("message.token", token);
                    span.setAttribute("message.topic", record.getRecord().topic());
                    span.setAttribute("message.offset", record.getRecord().offset());
                    span.setAttribute("message.timestamp", record.getRecord().timestamp());

                    InputMessage message = new InputMessage(record.getRecord().key(), token, payload, record.getRecord().timestamp());

                    final RxSingleHandler<InputMessage, ?> handler = messageHandlers.get(payload.getType());

                    if (handler == null) {
                        return;
                    }

                    log.trace("Received message from topic {}: {}", topicPartition.topic(), message);

                    final List<Tag> tags = List.of(
                            Tag.of("topic", topicPartition.topic()),
                            Tag.of("type", payload.getType())
                    );

                    registry.counter(VERTX_KAFKA_CONSUMER_RECORD_TYPE_COUNT, tags).increment();

                    registry.summary(VERTX_KAFKA_CONSUMER_RECORD_LAG_SECONDS, tags)
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

                registry.counter(VERTX_KAFKA_CONSUMER_ERROR_COUNT, tags).increment();

                log.error("Failed to process record: {}", record.getRecord().key());

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
                records.stream().map(record -> convertRecord(topicPartition, record))
                        .collect(Collectors.groupingBy(InputMessage::getKey))
                        .values()
                        .forEach(groupedMessages -> {
                            Map<String, List<InputMessage>> messagesByType = groupedMessages.stream()
                                    .peek(message -> log.trace("Received message from topic {}: {}", topicPartition.topic(), message))
                                    .collect(Collectors.groupingBy(message -> message.getValue().getType()));

                            messagesByType.keySet().forEach(type -> {
                                final List<InputMessage> messages = messagesByType.get(type);

                                if (messages.isEmpty()) {
                                    return;
                                }

                                final Span messageSpan = tracer.spanBuilder("Received messages " + type).startSpan();

                                try (Scope scope = messageSpan.makeCurrent()) {
                                    final Span span = Span.current();

                                    span.setAttribute("message.type", type);
                                    span.setAttribute("message.count", messages.size());

                                    final RxSingleHandler<List<InputMessage>, ?> handler = messageHandlers.get(type);

                                    if (handler == null) {
                                        return;
                                    }

                                    final List<Tag> tags = List.of(
                                            Tag.of("topic", topicPartition.topic()),
                                            Tag.of("type", type)
                                    );

                                    registry.counter(VERTX_KAFKA_CONSUMER_RECORD_TYPE_COUNT, tags).increment(messages.size());

                                    registry.summary(VERTX_KAFKA_CONSUMER_RECORD_LAG_SECONDS, tags)
                                            .record((System.currentTimeMillis() - messages.get(0).getTimestamp()) / 1000.0);

                                    handler.handleSingle(messages)
                                            .subscribeOn(Schedulers.immediate())
                                            .toCompletable()
                                            .await();
                                } finally {
                                    messageSpan.end();
                                }
                            });
                         });
            } catch (Exception e) {
                throw new KafkaMessagePolling.RecordProcessingException(records.get(0).getRecord());
            }
        }

        private InputMessage convertRecord(TopicPartition topicPartition, KafkaRecordsQueue.QueuedRecord record) {
            try {
                final Payload payload = record.getPayload();

                final String token = Token.from(record.getRecord().timestamp(), record.getRecord().offset());

                return new InputMessage(record.getRecord().key(), token, payload, record.getRecord().timestamp());
            } catch (Exception e) {
                final List<Tag> tags = List.of(
                        Tag.of("topic", topicPartition.topic())
                );

                registry.counter(VERTX_KAFKA_CONSUMER_ERROR_COUNT, tags).increment();

                log.error("Failed to convert record: {}", record.getRecord().key());

                throw new KafkaMessagePolling.RecordProcessingException(record.getRecord());
            }
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
