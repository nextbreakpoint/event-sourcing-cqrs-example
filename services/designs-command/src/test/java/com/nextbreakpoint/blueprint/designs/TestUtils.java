package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.MessagePayload;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import io.restassured.config.LogConfig;
import io.restassured.config.RedirectConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.nextbreakpoint.blueprint.designs.TestConstants.MESSAGE_SOURCE;

public class TestUtils {
    private TestUtils() {}

    @NotNull
    public static RestAssuredConfig getRestAssuredConfig() {
        final SSLConfig sslConfig = new SSLConfig().allowAllHostnames().and().relaxedHTTPSValidation();
        final RedirectConfig redirectConfig = new RedirectConfig().followRedirects(false);
        final LogConfig logConfig = new LogConfig().enableLoggingOfRequestAndResponseIfValidationFails();
        return RestAssuredConfig.newConfig().redirect(redirectConfig).sslConfig(sslConfig).logConfig(logConfig);
    }

    @NotNull
    public static Map<String, String> createPostData(String manifest, String metadata, String script) {
        final Map<String, String> data = new HashMap<>();
        data.put("manifest", manifest);
        data.put("metadata", metadata);
        data.put("script", script);
        return data;
    }

    @NotNull
    public static <T> InputMessage<T> createInputMessage(String messageKey, String messageType, UUID messageId, T messageData, String messageToken, LocalDateTime messageTime) {
        final MessagePayload<T> payload = MessagePayload.<T>builder()
                .withUuid(messageId)
                .withData(messageData)
                .withType(messageType)
                .withSource(MESSAGE_SOURCE)
                .build();

        return InputMessage.<T>builder()
                .withKey(messageKey)
                .withValue(payload)
                .withToken(messageToken)
                .withTimestamp(messageTime.toInstant(ZoneOffset.UTC).toEpochMilli())
                .build();
    }

    @NotNull
    public static <T> OutputMessage<T> createOutputMessage(String messageKey, String messageType, UUID messageId, T messageData) {
        final MessagePayload<T> payload = MessagePayload.<T>builder()
                .withUuid(messageId)
                .withData(messageData)
                .withType(messageType)
                .withSource(MESSAGE_SOURCE)
                .build();

        return OutputMessage.<T>builder()
                .withKey(messageKey)
                .withValue(payload)
                .build();
    }
}
