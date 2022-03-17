package com.nextbreakpoint.blueprint.common.vertx;

import com.nextbreakpoint.blueprint.common.core.*;
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
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumer;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumerRecords;
import io.vertx.rxjava.kafka.client.producer.KafkaHeader;
import rx.Single;
import rx.schedulers.Schedulers;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class KafkaPolling {
    private static final Logger logger = LoggerFactory.getLogger(KafkaPolling.class.getName());

    private final KafkaConsumer<String, String> kafkaConsumer;

    private final Map<String, RxSingleHandler<InputMessage, ?>> messageHandlers;

    private final KafkaRecordsQueue queue;

    private final int latency;

    private final int maxRecords;

    private final Set<TopicPartition> suspendedPartitions = new HashSet<>();

    private long timestamp;

    private Thread pollingThread;

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

    public KafkaPolling(KafkaConsumer<String, String> kafkaConsumer, Map<String, RxSingleHandler<InputMessage, ?>> messageHandlers) {
        this(kafkaConsumer, messageHandlers, KafkaRecordsQueue.Simple.create(), -1, 10);
    }

    public KafkaPolling(KafkaConsumer<String, String> kafkaConsumer, Map<String, RxSingleHandler<InputMessage, ?>> messageHandlers, KafkaRecordsQueue queue, int latency, int maxRecords) {
        this.kafkaConsumer = Objects.requireNonNull(kafkaConsumer);
        this.messageHandlers = Objects.requireNonNull(messageHandlers);
        this.queue = Objects.requireNonNull(queue);
        this.latency = latency;
        this.maxRecords = maxRecords;

        tracer = GlobalOpenTelemetry.getTracer("com.nextbreakpoint.blueprint");
    }

    public void startPolling(String name) {
        if (pollingThread != null) {
            return;
        }

        pollingThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    if (queue.size() < maxRecords) {
                        enqueueRecords(pollRecords());

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
                } catch (Exception e) {
                    logger.error("Error occurred while consuming messages", e);

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
                logger.warn("Can't stop polling thread", e);
            }

            pollingThread = null;
        }
    }

    private void enqueueRecords(KafkaConsumerRecords<String, String> records) {
        for (int i = 0; i < records.size(); i++) {
            final KafkaConsumerRecord<String, String> record = records.recordAt(i);

            final TopicPartition topicPartition = new TopicPartition(record.topic(), record.partition());

            try {
                if (suspendedPartitions.contains(topicPartition)) {
                    logger.debug("Skipping record " + record.key() + " from suspended partition (" + topicPartition + ")");

                    continue;
                }

                if (record.value() == null) {
                    logger.debug("Skipping tombstone record " + record.key() + " from partition (" + topicPartition + ")");

                    queue.deleteRecord(record);

                    continue;
                }

                final Payload payload = Json.decodeValue(record.value(), Payload.class);

                final RxSingleHandler<InputMessage, ?> handler = messageHandlers.get(payload.getType());

                if (handler == null) {
                    continue;
                }

                if (queue.size() == 0) {
                    timestamp = System.currentTimeMillis();
                }

                queue.addRecord(record);
            } catch (Exception e) {
                logger.error("Failed to process record: " + record.key());

                suspendedPartitions.add(topicPartition);

                retryPartition(record, topicPartition);
            }
        }

        suspendedPartitions.clear();
    }

    private void processRecords() {
        final long currentTimeMillis = System.currentTimeMillis();

        if (queue.size() > 0 && currentTimeMillis - timestamp > latency) {
            logger.debug("Received " + queue.size() + " " + (queue.size() > 0 ? "messages" : "message"));

            queue.getRecords().forEach(this::processRecord);

            queue.clear();

            commitOffsets();
        }
    }

    private void processRecord(KafkaConsumerRecord<String, String> record) {
        final TopicPartition topicPartition = new TopicPartition(record.topic(), record.partition());

        try {
            if (suspendedPartitions.contains(topicPartition)) {
                logger.debug("Skipping record " + record.key() + " from suspended partition (" + topicPartition + ")");

                return;
            }

            final Map<String, String> headers = record.headers().stream()
                    .collect(Collectors.toMap(KafkaHeader::key, kafkaHeader -> getString(kafkaHeader.value())));

            final Payload payload = Json.decodeValue(record.value(), Payload.class);

            final RxSingleHandler<InputMessage, ?> handler = messageHandlers.get(payload.getType());

            if (handler == null) {
                return;
            }

            final Context extractedContext = W3CTraceContextPropagator.getInstance().extract(Context.current(), headers, getter);

            final Span messageSpan = tracer.spanBuilder("Received message " + payload.getType()).setParent(extractedContext).startSpan();

            try (Scope scope = messageSpan.makeCurrent()) {
                final Span span = Span.current();

                final String token = Token.from(record.timestamp(), record.offset());

                Tracing tracing = Tracing.of(span.getSpanContext().getTraceId(), span.getSpanContext().getSpanId());

                span.setAttribute("message.source", payload.getSource());
                span.setAttribute("message.type", payload.getType());
                span.setAttribute("message.uuid", payload.getUuid().toString());
                span.setAttribute("message.key", record.key());
                span.setAttribute("message.token", token);
                span.setAttribute("message.topic", record.topic());
                span.setAttribute("message.offset", record.offset());
                span.setAttribute("message.timestamp", record.timestamp());

                final InputMessage message = new InputMessage(record.key(), token, payload, tracing, record.timestamp());

                logger.debug("Received message: " + message);

                Single.just(Context.current())
                        .subscribeOn(Schedulers.computation())
                        .map(Context::makeCurrent)
                        .toCompletable()
                        .await();

                handler.handleSingle(message)
                        .toCompletable()
                        .await();
            } finally {
                messageSpan.end();
            }
        } catch (Exception e) {
            logger.error("Failed to process record: " + record.key());

            suspendedPartitions.add(topicPartition);

            retryPartition(record, topicPartition);
        }

        suspendedPartitions.clear();
    }

    private String getString(Buffer value) {
        return value != null ? value.toString() : null;
    }

    private KafkaConsumerRecords<String, String> pollRecords() {
        return kafkaConsumer.fetch(Math.min(10, maxRecords))
                .rxPoll(Duration.ofSeconds(10))
                .subscribeOn(Schedulers.computation())
                .doOnError(err -> logger.error("Failed to consume records", err))
                .toBlocking()
                .value();
    }

    private void commitOffsets() {
        kafkaConsumer.rxCommit()
                .subscribeOn(Schedulers.computation())
                .doOnError(err -> logger.error("Failed to commit offsets", err))
                .toCompletable()
                .await();
    }

    private void retryPartition(KafkaConsumerRecord<String, String> record, TopicPartition topicPartition) {
        kafkaConsumer.rxPause(topicPartition)
                .subscribeOn(Schedulers.computation())
                .flatMap(x -> kafkaConsumer.rxSeek(topicPartition, record.offset()))
                .delay(10, TimeUnit.SECONDS)
                .flatMap(x -> kafkaConsumer.rxResume(topicPartition))
                .doOnError(err -> logger.error("Failed to resume partition (" + topicPartition + ")", err))
                .toCompletable()
                .await();
    }
}
