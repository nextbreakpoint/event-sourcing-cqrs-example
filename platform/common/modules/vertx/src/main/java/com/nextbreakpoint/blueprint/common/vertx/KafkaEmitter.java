package com.nextbreakpoint.blueprint.common.vertx;

import com.nextbreakpoint.blueprint.common.core.*;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.rxjava.kafka.client.producer.KafkaHeader;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import io.vertx.rxjava.kafka.client.producer.KafkaProducerRecord;
import rx.Single;

import java.util.*;
import java.util.stream.Collectors;

public class KafkaEmitter implements MessageEmitter {
    private final Logger logger = LoggerFactory.getLogger(KafkaEmitter.class.getName());

    private final KafkaProducer<String, String> producer;
    private final String topicName;
    private final int retries;

    private final Tracer tracer;

    private final TextMapSetter<Map<String, String>> setter = new TextMapSetter<>() {
        @Override
        public void set(Map<String, String> headers, String key, String value) {
            headers.put(key, value);
        }
    };

    public KafkaEmitter(KafkaProducer<String, String> producer, String topicName, int retries) {
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
                .doOnEach(notification -> logger.debug("Sending message to topic " + topicName + ": " + notification.getValue()))
                .map(outputMessage -> writeRecord(outputMessage, topicName));
    }

    private Void writeRecord(OutputMessage message, String topicName) {
        final Payload payload = message.getValue();

        final Span messageSpan = tracer.spanBuilder("Sending message " + payload.getType()).startSpan();

        try (Scope scope = messageSpan.makeCurrent()) {
            final Span span = Span.current();

            final KafkaProducerRecord<String, String> record = createRecord(message, topicName);

            span.setAttribute("message.source", payload.getSource());
            span.setAttribute("message.type", payload.getType());
            span.setAttribute("message.uuid", payload.getUuid().toString());
            span.setAttribute("message.key", record.key());
            span.setAttribute("message.topic", topicName);

            producer.write(record);
        } finally {
            messageSpan.end();
        }

        return null;
    }

    private KafkaProducerRecord<String, String> createRecord(OutputMessage message, String topicName) {
        final KafkaProducerRecord<String, String> record = KafkaProducerRecord.create(topicName, message.getKey(), Json.encodeValue(message.getValue()));
        final Map<String, String> headers = new HashMap<>();
        W3CTraceContextPropagator.getInstance().inject(Context.current(), headers, setter);
        record.addHeaders(headers.entrySet().stream().map(e -> KafkaHeader.header(e.getKey(), e.getValue())).collect(Collectors.toList()));
        return record;
    }

    @Override
    public String getTopicName() {
        return topicName;
    }
}
