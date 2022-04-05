package com.nextbreakpoint.blueprint.common.vertx;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Payload;
import com.nextbreakpoint.blueprint.common.core.Token;
import com.nextbreakpoint.blueprint.common.core.Tracing;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.kafka.client.common.TopicPartition;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.kafka.client.producer.KafkaHeader;
import rx.Single;
import rx.schedulers.Schedulers;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public interface KafkaRecordsConsumer {
    void consumeRecords(TopicPartition topicPartition, List<KafkaRecordsQueue.QueuedRecord> records);

    class Simple implements KafkaRecordsConsumer {
        private static final Logger logger = LoggerFactory.getLogger(Simple.class.getName());

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
            records.stream().map(this::convertRecord).forEach(this::consumeMessage);
        }

        private MessageAndRecord convertRecord(KafkaRecordsQueue.QueuedRecord record) {
            try {
                final Payload payload = record.getPayload();

                final Map<String, String> headers = record.getRecord().headers().stream()
                        .collect(Collectors.toMap(KafkaHeader::key, kafkaHeader -> getString(kafkaHeader.value())));

                final Context extractedContext = W3CTraceContextPropagator.getInstance().extract(Context.current(), headers, getter);

                final Span messageSpan = tracer.spanBuilder("Received message " + payload.getType()).setParent(extractedContext).startSpan();

                try (Scope scope = messageSpan.makeCurrent()) {
                    final Span span = Span.current();

                    final String token = Token.from(record.getRecord().timestamp(), record.getRecord().offset());

                    Tracing tracing = Tracing.of(span.getSpanContext().getTraceId(), span.getSpanContext().getSpanId());

                    span.setAttribute("message.source", payload.getSource());
                    span.setAttribute("message.type", payload.getType());
                    span.setAttribute("message.uuid", payload.getUuid().toString());
                    span.setAttribute("message.key", record.getRecord().key());
                    span.setAttribute("message.token", token);
                    span.setAttribute("message.topic", record.getRecord().topic());
                    span.setAttribute("message.offset", record.getRecord().offset());
                    span.setAttribute("message.timestamp", record.getRecord().timestamp());

                    Single.just(Context.current())
                            .subscribeOn(Schedulers.computation())
                            .map(Context::makeCurrent)
                            .toCompletable()
                            .await();

                    return new MessageAndRecord(record, new InputMessage(record.getRecord().key(), token, payload, tracing, record.getRecord().timestamp()));
                } finally {
                    messageSpan.end();
                }
            } catch (Exception e) {
                logger.error("Failed to process record: " + record.getRecord().key());

                throw new KafkaPolling.RecordProcessingException(record.getRecord());
            }
        }

        private void consumeMessage(MessageAndRecord messageAndRecord) {
            try {
                final RxSingleHandler<InputMessage, ?> handler = messageHandlers.get(messageAndRecord.message.getValue().getType());

                if (handler == null) {
                    return;
                }

                handler.handleSingle(messageAndRecord.message)
                        .toCompletable()
                        .await();
            } catch (Exception e) {
                logger.error("Failed to process message: " + messageAndRecord.record.getRecord().key());

                throw new KafkaPolling.RecordProcessingException(messageAndRecord.record.getRecord());
            }
        }

        private String getString(Buffer value) {
            return value != null ? value.toString() : null;
        }
    }

    class Buffered implements KafkaRecordsConsumer {
        private static final Logger logger = LoggerFactory.getLogger(Buffered.class.getName());

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
                records.stream().map(this::convertRecord).map(MessageAndRecord::getMessage)
                        .collect(Collectors.groupingBy(InputMessage::getKey)).values().forEach(groupedMessages -> {
                            Map<String, List<InputMessage>> messagesByType = groupedMessages.stream()
                                    .collect(Collectors.groupingBy(message -> message.getValue().getType()));

                            messagesByType.keySet().forEach(type -> {
                                final List<InputMessage> messages = messagesByType.get(type);

                                final RxSingleHandler<List<InputMessage>, ?> handler = messageHandlers.get(type);

                                if (handler == null) {
                                    return;
                                }

                                handler.handleSingle(messages)
                                        .toCompletable()
                                        .await();
                            });
                });
            } catch (Exception e) {
                throw new KafkaPolling.RecordProcessingException(records.get(0).getRecord());
            }
        }

        private MessageAndRecord convertRecord(KafkaRecordsQueue.QueuedRecord record) {
            try {
                final Payload payload = record.getPayload();

                final Map<String, String> headers = record.getRecord().headers().stream()
                        .collect(Collectors.toMap(KafkaHeader::key, kafkaHeader -> getString(kafkaHeader.value())));

                final Context extractedContext = W3CTraceContextPropagator.getInstance().extract(Context.current(), headers, getter);

                final Span messageSpan = tracer.spanBuilder("Received message " + payload.getType()).setParent(extractedContext).startSpan();

                try (Scope scope = messageSpan.makeCurrent()) {
                    final Span span = Span.current();

                    final String token = Token.from(record.getRecord().timestamp(), record.getRecord().offset());

                    Tracing tracing = Tracing.of(span.getSpanContext().getTraceId(), span.getSpanContext().getSpanId());

                    span.setAttribute("message.source", payload.getSource());
                    span.setAttribute("message.type", payload.getType());
                    span.setAttribute("message.uuid", payload.getUuid().toString());
                    span.setAttribute("message.key", record.getRecord().key());
                    span.setAttribute("message.token", token);
                    span.setAttribute("message.topic", record.getRecord().topic());
                    span.setAttribute("message.offset", record.getRecord().offset());
                    span.setAttribute("message.timestamp", record.getRecord().timestamp());

                    Single.just(Context.current())
                            .subscribeOn(Schedulers.computation())
                            .map(Context::makeCurrent)
                            .toCompletable()
                            .await();

                    return new MessageAndRecord(record, new InputMessage(record.getRecord().key(), token, payload, tracing, record.getRecord().timestamp()));
                } finally {
                    messageSpan.end();
                }
            } catch (Exception e) {
                logger.error("Failed to process record: " + record.getRecord().key());

                throw new KafkaPolling.RecordProcessingException(record.getRecord());
            }
        }

        private String getString(Buffer value) {
            return value != null ? value.toString() : null;
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
