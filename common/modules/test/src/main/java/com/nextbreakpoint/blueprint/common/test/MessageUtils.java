package com.nextbreakpoint.blueprint.common.test;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.MessagePayload;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

public class MessageUtils {
    private MessageUtils() {}

    public static <T> InputMessage<T> createInputMessage(String messageSource, String messageKey, String messageType, UUID messageId, T messageData, String messageToken, LocalDateTime messageTime) {
        final MessagePayload<T> payload = MessagePayload.<T>builder()
                .withUuid(messageId)
                .withData(messageData)
                .withType(messageType)
                .withSource(messageSource)
                .build();

        return InputMessage.<T>builder()
                .withKey(messageKey)
                .withValue(payload)
                .withToken(messageToken)
                .withTimestamp(messageTime.toInstant(ZoneOffset.UTC).toEpochMilli())
                .build();
    }

    public static <T> OutputMessage<T> createOutputMessage(String messageSource, String messageKey, String messageType, UUID messageId, T messageData) {
        final MessagePayload<T> payload = MessagePayload.<T>builder()
                .withUuid(messageId)
                .withData(messageData)
                .withType(messageType)
                .withSource(messageSource)
                .build();

        return OutputMessage.<T>builder()
                .withKey(messageKey)
                .withValue(payload)
                .build();
    }
}
