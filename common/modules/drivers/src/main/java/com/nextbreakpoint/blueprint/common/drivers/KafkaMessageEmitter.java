package com.nextbreakpoint.blueprint.common.drivers;

import com.nextbreakpoint.blueprint.common.core.Header;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.MessageEmitter;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.core.OutputRecord;
import com.nextbreakpoint.blueprint.common.core.MessagePayload;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapSetter;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import rx.Single;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Log4j2
public class KafkaMessageEmitter<T, R> implements MessageEmitter<R> {
    public static final String KAFKA_EMITTER_ERROR_COUNT = "kafka_emitter_error_count";
    public static final String KAFKA_EMITTER_RECORD_TYPE_COUNT = "kafka_emitter_record_type_count";

    private final KafkaProducer<String, T> producer;
    private final Mapper<OutputRecord<R>, ProducerRecord<String, T>> recordMapper;
    private final MeterRegistry registry;
    private final String topicName;
    private final int retries;

    private final Tracer tracer;

    private final TextMapSetter<Map<String, String>> setter = Map::put;

    public KafkaMessageEmitter(KafkaProducer<String, T> producer, Mapper<OutputRecord<R>, ProducerRecord<String, T>> recordMapper, MeterRegistry registry, String topicName, int retries) {
        this.producer = Objects.requireNonNull(producer);
        this.recordMapper = Objects.requireNonNull(recordMapper);
        this.registry = Objects.requireNonNull(registry);
        this.topicName = Objects.requireNonNull(topicName);
        this.retries = retries;

        tracer = GlobalOpenTelemetry.getTracer("com.nextbreakpoint.blueprint");
    }

    @Override
    public Single<Void> send(OutputMessage<R> message) {
        return send(message, topicName);
    }

    @Override
    public Single<Void> send(OutputMessage<R> message, String topicName) {
        return Single.just(message)
                .doOnEach(notification -> log.trace("Sending message to topic {}: {}", topicName, notification.getValue()))
                .map(outputMessage -> writeRecord(outputMessage, topicName))
                .doOnError(err -> log.error("Error occurred while writing record. Retrying...", err))
                .retry(retries)
                .map(result -> null);
    }

    private RecordMetadata writeRecord(OutputMessage<R> message, String topicName) {
        try {
            final MessagePayload payload = message.getValue();

            final Span messageSpan = tracer.spanBuilder("Sending message " + payload.getType()).startSpan();

            try (Scope scope = messageSpan.makeCurrent()) {
                final Span span = Span.current();

                final ProducerRecord<String, T> record = createRecord(message, topicName);

                span.setAttribute("message.source", payload.getSource());
                span.setAttribute("message.type", payload.getType());
                span.setAttribute("message.uuid", payload.getUuid().toString());
                span.setAttribute("message.key", record.key());
                span.setAttribute("message.topic", topicName);

                final List<Tag> recordTags = List.of(
                        Tag.of("topic", topicName),
                        Tag.of("source", payload.getSource()),
                        Tag.of("type", payload.getType())
                );

                registry.counter(KAFKA_EMITTER_RECORD_TYPE_COUNT, recordTags).increment();

                return producer.send(record).get(2000, TimeUnit.SECONDS);
            } finally {
                messageSpan.end();
            }
        } catch (Exception e) {
            final List<Tag> tags = List.of(
                    Tag.of("topic", topicName)
            );

            registry.counter(KAFKA_EMITTER_ERROR_COUNT, tags).increment();

            throw new RuntimeException(e);
        }
    }

    private ProducerRecord<String, T> createRecord(OutputMessage<R> message, String topicName) {
        final Map<String, String> headers = new HashMap<>();

        W3CTraceContextPropagator.getInstance().inject(Context.current(), headers, setter);

        final List<Header> recordHeaders = headers.entrySet().stream()
                .map(e -> Header.builder().withKey(e.getKey()).withValue(e.getValue()).build())
                .toList();

        final OutputRecord<R> record = OutputRecord.<R>builder()
                .withKey(message.getKey())
                .withTopicName(topicName)
                .withPayload(message.getValue())
                .withHeaders(recordHeaders)
                .build();

        return recordMapper.transform(record);
    }

    @Override
    public String getTopicName() {
        return topicName;
    }
}
