package com.nextbreakpoint.blueprint.common.drivers;

import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.core.Payload;
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
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import rx.Single;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Log4j2
public class MessageEmitter implements com.nextbreakpoint.blueprint.common.core.MessageEmitter {
    private final KafkaProducer<String, String> producer;
    private final String topicName;
    private final int retries;

    private final Tracer tracer;

    private final TextMapSetter<Map<String, String>> setter = Map::put;

    public MessageEmitter(KafkaProducer<String, String> producer, String topicName, int retries) {
        this.producer = Objects.requireNonNull(producer);
        this.topicName = Objects.requireNonNull(topicName);
        this.retries = retries;

        tracer = GlobalOpenTelemetry.getTracer("com.nextbreakpoint.blueprint");
    }

    @Override
    public Single<Void> send(OutputMessage message) {
        return send(message, topicName);
    }

    @Override
    public Single<Void> send(OutputMessage message, String topicName) {
        return Single.just(message)
                .doOnEach(notification -> log.debug("Sending message to topic " + topicName + ": " + notification.getValue()))
                .map(outputMessage -> writeRecord(outputMessage, topicName))
                .doOnError(err -> log.error("Error occurred while writing record. Retrying...", err))
                .retry(retries)
                .map(result -> null);
    }

    private RecordMetadata writeRecord(OutputMessage message, String topicName) {
        try {
            final Payload payload = message.getValue();

            final Span messageSpan = tracer.spanBuilder("Sending message " + payload.getType()).startSpan();

            try (Scope scope = messageSpan.makeCurrent()) {
                final Span span = Span.current();

                final ProducerRecord<String, String> record = createRecord(message, topicName);

                span.setAttribute("message.source", payload.getSource());
                span.setAttribute("message.type", payload.getType());
                span.setAttribute("message.uuid", payload.getUuid().toString());
                span.setAttribute("message.key", record.key());
                span.setAttribute("message.topic", topicName);

                return producer.send(record).get(2000, TimeUnit.SECONDS);
            } finally {
                messageSpan.end();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ProducerRecord<String, String> createRecord(OutputMessage message, String topicName) {
        final Map<String, String> headers = new HashMap<>();

        W3CTraceContextPropagator.getInstance().inject(Context.current(), headers, setter);

        final List<Header> recordHeaders = headers.entrySet().stream()
                .map(e -> new RecordHeader(e.getKey(), e.getValue().getBytes()))
                .collect(Collectors.toList());

        return new ProducerRecord<>(topicName, null, message.getKey(), Json.encodeValue(message.getValue()), recordHeaders);
    }

    @Override
    public String getTopicName() {
        return topicName;
    }
}
