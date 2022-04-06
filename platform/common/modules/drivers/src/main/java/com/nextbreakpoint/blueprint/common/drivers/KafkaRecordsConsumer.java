package com.nextbreakpoint.blueprint.common.drivers;

import com.nextbreakpoint.blueprint.common.core.*;
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

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public interface KafkaRecordsConsumer {
    void consumeRecords(TopicPartition topicPartition, List<KafkaRecordsQueue.QueuedRecord> records);

    @Log4j2
    class Simple implements KafkaRecordsConsumer {
        private final Map<String, RxSingleHandler<InputMessage, ?>> messageHandlers;

        private final Tracer tracer;

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

        public static KafkaRecordsConsumer create(Map<String, RxSingleHandler<InputMessage, ?>> messageHandlers) {
            return new Simple(messageHandlers);
        }

        private Simple(Map<String, RxSingleHandler<InputMessage, ?>> messageHandlers) {
            this.messageHandlers = Objects.requireNonNull(messageHandlers);

            tracer = GlobalOpenTelemetry.getTracer("com.nextbreakpoint.blueprint");
        }

        @Override
        public void consumeRecords(TopicPartition topicPartition, List<KafkaRecordsQueue.QueuedRecord> records) {
            records.forEach(this::consumeMessage);
        }

        private void consumeMessage(KafkaRecordsQueue.QueuedRecord record) {
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

                    handler.handleSingle(message)
                            .subscribeOn(Schedulers.immediate())
                            .toCompletable()
                            .await();
                } finally {
                    messageSpan.end();
                }
            } catch (Exception e) {
                log.error("Failed to process record: " + record.getRecord().key());

                throw new KafkaPolling.RecordProcessingException(record.getRecord());
            }
        }

        private String getString(byte[] value) {
            return value != null ? new String(value) : null;
        }
    }

    @Log4j2
    class Buffered implements KafkaRecordsConsumer {
        private final Map<String, RxSingleHandler<List<InputMessage>, ?>> messageHandlers;

        private final Tracer tracer;

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

        public static KafkaRecordsConsumer create(Map<String, RxSingleHandler<List<InputMessage>, ?>> messageHandlers) {
            return new Buffered(messageHandlers);
        }

        public Buffered(Map<String, RxSingleHandler<List<InputMessage>, ?>> messageHandlers) {
            this.messageHandlers = Objects.requireNonNull(messageHandlers);

            tracer = GlobalOpenTelemetry.getTracer("com.nextbreakpoint.blueprint");
        }

        @Override
        public void consumeRecords(TopicPartition topicPartition, List<KafkaRecordsQueue.QueuedRecord> records) {
            try {
                records.stream().map(this::convertRecord)
                        .collect(Collectors.groupingBy(InputMessage::getKey))
                        .values()
                        .forEach(groupedMessages -> {
                            Map<String, List<InputMessage>> messagesByType = groupedMessages.stream()
                                    .collect(Collectors.groupingBy(message -> message.getValue().getType()));

                            messagesByType.keySet().forEach(type -> {
                                final List<InputMessage> messages = messagesByType.get(type);

                                final Span messageSpan = tracer.spanBuilder("Received messages " + type).startSpan();

                                try (Scope scope = messageSpan.makeCurrent()) {
                                    final Span span = Span.current();

                                    span.setAttribute("message.type", type);
                                    span.setAttribute("message.count", messages.size());

                                    final RxSingleHandler<List<InputMessage>, ?> handler = messageHandlers.get(type);

                                    if (handler == null) {
                                        return;
                                    }

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
                throw new KafkaPolling.RecordProcessingException(records.get(0).getRecord());
            }
        }

        private InputMessage convertRecord(KafkaRecordsQueue.QueuedRecord record) {
            try {
                final Payload payload = record.getPayload();

                final String token = Token.from(record.getRecord().timestamp(), record.getRecord().offset());

                return new InputMessage(record.getRecord().key(), token, payload, record.getRecord().timestamp());
            } catch (Exception e) {
                log.error("Failed to process record: " + record.getRecord().key());

                throw new KafkaPolling.RecordProcessingException(record.getRecord());
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
