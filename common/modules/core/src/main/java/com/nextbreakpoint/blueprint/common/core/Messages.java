package com.nextbreakpoint.blueprint.common.core;

import java.util.function.Function;

public class Messages {
    private Messages() {}

    public static <T, R> InputMessage<T> asSpecificMessage(InputMessage<R> message, Function<R, T> mapper) {
        return createMessage(message, mapper);
    }

    public static <T, R> OutputMessage<T> asSpecificMessage(OutputMessage<R> message, Function<R, T> mapper) {
        return createMessage(message, mapper);
    }

    private static <T, R> InputMessage<T> createMessage(InputMessage<R> message, Function<R, T> mapper) {
        return InputMessage.<T>builder()
                .withKey(message.getKey())
                .withToken(message.getToken())
                .withValue(createPayload(message, mapper))
                .withTimestamp(message.getTimestamp())
                .build();
    }

    private static <T, R> OutputMessage<T> createMessage(OutputMessage<R> message, Function<R, T> mapper) {
        return OutputMessage.<T>builder()
                .withKey(message.getKey())
                .withValue(createPayload(message, mapper))
                .build();
    }

    private static <T, R> MessagePayload<T> createPayload(InputMessage<R> message, Function<R, T> mapper) {
        return MessagePayload.<T>builder()
                .withUuid(message.getValue().getUuid())
                .withType(message.getValue().getType())
                .withSource(message.getValue().getSource())
                .withData(mapper.apply(message.getValue().getData()))
                .build();
    }

    private static <T, R> MessagePayload<T> createPayload(OutputMessage<R> message, Function<R, T> mapper) {
        return MessagePayload.<T>builder()
                .withUuid(message.getValue().getUuid())
                .withType(message.getValue().getType())
                .withSource(message.getValue().getSource())
                .withData(mapper.apply(message.getValue().getData()))
                .build();
    }
}
