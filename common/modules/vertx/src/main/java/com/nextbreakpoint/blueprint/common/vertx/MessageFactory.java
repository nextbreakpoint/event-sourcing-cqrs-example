package com.nextbreakpoint.blueprint.common.vertx;

import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.core.MessagePayload;
import org.apache.avro.specific.SpecificRecord;

import java.util.Objects;
import java.util.UUID;

public class MessageFactory<T extends SpecificRecord> {
    private final String source;

    private MessageFactory(String source) {
        this.source = Objects.requireNonNull(source);
    }

    public static <T extends SpecificRecord> MessageFactory<T> of(String source) {
        return new MessageFactory<>(source);
    }

    public OutputMessage<T> createOutputMessage(String key, T data) {
        return OutputMessage.<T>builder()
                .withKey(key)
                .withValue(createPayload(data))
                .build();
    }

    private MessagePayload<T> createPayload(T value) {
        return MessagePayload.<T>builder()
                .withUuid(UUID.randomUUID())
                .withType(value.getSchema().getFullName())
                .withData(value)
                .withSource(source)
                .build();
    }
}
