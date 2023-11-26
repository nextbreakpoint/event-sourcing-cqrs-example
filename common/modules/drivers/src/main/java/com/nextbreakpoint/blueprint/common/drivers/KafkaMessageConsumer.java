package com.nextbreakpoint.blueprint.common.drivers;

import com.nextbreakpoint.blueprint.common.core.Header;
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
import rx.schedulers.Schedulers;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public interface KafkaMessageConsumer<T> {
    String KAFKA_CONSUMER_ERROR_COUNT = "kafka_consumer_error_count";
    String KAFKA_CONSUMER_RECORD_TYPE_COUNT = "kafka_consumer_record_type_count";
    String KAFKA_CONSUMER_RECORD_LATENCY_SECONDS = "kafka_consumer_record_latency_seconds";

    void consumeRecords(TopicPartition topicPartition, List<KafkaRecordsQueue.QueuedRecord<T>> records);

    @Log4j2
    class Simple<T> implements KafkaMessageConsumer<T> {
        private final Map<String, RxSingleHandler<InputMessage<T>, Void>> messageHandlers;

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

        public static <T> KafkaMessageConsumer<T> create(Map<String, RxSingleHandler<InputMessage<T>, Void>> messageHandlers, MeterRegistry registry) {
            return new Simple<>(messageHandlers, registry);
        }

        private Simple(Map<String, RxSingleHandler<InputMessage<T>, Void>> messageHandlers, MeterRegistry registry) {
            this.messageHandlers = Objects.requireNonNull(messageHandlers);
            this.registry = Objects.requireNonNull(registry);

            tracer = GlobalOpenTelemetry.getTracer("com.nextbreakpoint.blueprint");
        }

        @Override
        public void consumeRecords(TopicPartition topicPartition, List<KafkaRecordsQueue.QueuedRecord<T>> records) {
            records.forEach(record -> consumeMessage(topicPartition, record));
        }

        private void consumeMessage(TopicPartition topicPartition, KafkaRecordsQueue.QueuedRecord<T> record) {
            try {
                final RxSingleHandler<InputMessage<T>, ?> handler = messageHandlers.get(record.getRecord().getPayloadV2().getType());

                if (handler == null) {
                    return;
                }

                final String token = Token.from(record.getRecord().getTimestamp(), record.getRecord().getOffset());

                final InputMessage<T> message = InputMessage.<T>builder()
                        .withKey(record.getRecord().getKey())
                        .withToken(token)
                        .withValue(record.getRecord().getPayloadV2())
                        .withTimestamp(record.getRecord().getTimestamp())
                        .build();

                final Map<String, String> headers = record.getRecord().getHeaders().stream()
                        .collect(Collectors.toMap(Header::getKey, Header::getValue));

                final Context extractedContext = W3CTraceContextPropagator.getInstance().extract(Context.current(), headers, getter);

                final Span messageSpan = tracer.spanBuilder("Received message " + message.getValue().getType()).setParent(extractedContext).startSpan();

                try (Scope scope = messageSpan.makeCurrent()) {
                    final Span span = Span.current();

                    span.setAttribute("message.source", message.getValue().getSource());
                    span.setAttribute("message.type", message.getValue().getType());
                    span.setAttribute("message.uuid", message.getValue().getUuid().toString());
                    span.setAttribute("message.key", record.getRecord().getKey());
                    span.setAttribute("message.token", token);
                    span.setAttribute("message.topic", record.getRecord().getTopicName());
                    span.setAttribute("message.offset", record.getRecord().getOffset());
                    span.setAttribute("message.timestamp", record.getRecord().getTimestamp());

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

                log.error("Failed to consume 1 record: {}", record.getRecord().getKey());

                throw new KafkaMessagePolling.RecordProcessingException(record.getRecord());
            }
        }
    }

    @Log4j2
    class Buffered<T> implements KafkaMessageConsumer<T> {
        private final Map<String, RxSingleHandler<List<InputMessage<T>>, Void>> messageHandlers;

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

        public static <T> KafkaMessageConsumer<T> create(Map<String, RxSingleHandler<List<InputMessage<T>>, Void>> messageHandlers, MeterRegistry registry) {
            return new Buffered<>(messageHandlers, registry);
        }

        public Buffered(Map<String, RxSingleHandler<List<InputMessage<T>>, Void>> messageHandlers, MeterRegistry registry) {
            this.messageHandlers = Objects.requireNonNull(messageHandlers);
            this.registry = Objects.requireNonNull(registry);

            tracer = GlobalOpenTelemetry.getTracer("com.nextbreakpoint.blueprint");
        }

        @Override
        public void consumeRecords(TopicPartition topicPartition, List<KafkaRecordsQueue.QueuedRecord<T>> records) {
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

        private InputMessage<T> convertRecord(KafkaRecordsQueue.QueuedRecord<T> record) {
            final String token = Token.from(record.getRecord().getTimestamp(), record.getRecord().getOffset());

            return InputMessage.<T>builder()
                    .withKey(record.getRecord().getKey())
                    .withToken(token)
                    .withValue(record.getRecord().getPayloadV2())
                    .withTimestamp(record.getRecord().getTimestamp())
                    .build();
        }

        private void consumeMessage(TopicPartition topicPartition, List<KafkaRecordsQueue.QueuedRecord<T>> records, List<InputMessage<T>> groupedMessages) {
            final Map<String, List<InputMessage<T>>> messagesByType = groupedMessages.stream()
                    .peek(message -> log.trace("Received message from topic {}: {}", topicPartition.topic(), message))
                    .collect(Collectors.groupingBy(message -> message.getValue().getType()));

            messagesByType.keySet().forEach(type -> {
                final List<InputMessage<T>> messages = messagesByType.get(type);

                if (messages.isEmpty()) {
                    return;
                }

                final RxSingleHandler<List<InputMessage<T>>, ?> handler = messageHandlers.get(type);

                if (handler == null) {
                    return;
                }

                final Map<String, String> headers = records.get(0).getRecord().getHeaders().stream()
                        .collect(Collectors.toMap(Header::getKey, Header::getValue));

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
    }
}
