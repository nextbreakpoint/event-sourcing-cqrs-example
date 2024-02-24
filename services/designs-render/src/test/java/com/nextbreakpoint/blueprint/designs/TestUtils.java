package com.nextbreakpoint.blueprint.designs;

import au.com.dius.pact.core.model.V4Interaction;
import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.avro.TileRenderRequested;
import com.nextbreakpoint.blueprint.common.test.KafkaRecord;
import com.nextbreakpoint.blueprint.common.test.PayloadUtils;
import io.restassured.config.LogConfig;
import io.restassured.config.RedirectConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import org.apache.avro.specific.SpecificRecord;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class TestUtils {
    private TestUtils() {}

    @NotNull
    public static String createRenderKey(TileRenderRequested event) {
        return "%s/%s/%d/%04d%04d".formatted(event.getDesignId(), event.getCommandId(), event.getLevel(), event.getRow(), event.getCol());
    }

    @NotNull
    public static String createTileKey(TileRenderRequested event) {
        return "tiles/%s/%d/%04d%04d.png".formatted(event.getChecksum(), event.getLevel(), event.getRow(), event.getCol());
    }

    @NotNull
    public static String createCacheKey(String checksum) {
        return "cache/%s.png".formatted(checksum);
    }

    @NotNull
    public static <T extends SpecificRecord> OutputMessage<T> toOutputMessage(V4Interaction.AsynchronousMessage message, Class<T> clazz) {
        final String json = message.getContents().getContents().valueAsString();
        final KafkaRecord kafkaRecord = Json.decodeValue(json, KafkaRecord.class);

        return OutputMessage.<T>builder()
                .withKey(kafkaRecord.getKey())
                .withValue(PayloadUtils.mapToPayload(kafkaRecord.getValue(), clazz))
                .build();
    }

    @NotNull
    public static RestAssuredConfig getRestAssuredConfig() {
        final SSLConfig sslConfig = new SSLConfig().allowAllHostnames().and().relaxedHTTPSValidation();
        final RedirectConfig redirectConfig = new RedirectConfig().followRedirects(false);
        final LogConfig logConfig = new LogConfig().enableLoggingOfRequestAndResponseIfValidationFails();
        return RestAssuredConfig.newConfig().redirect(redirectConfig).sslConfig(sslConfig).logConfig(logConfig);
    }

    @NotNull
    public static Map<String, Object> createPostData(String manifest, String metadata, String script) {
        final Map<String, Object> data = new HashMap<>();
        data.put("manifest", manifest);
        data.put("metadata", metadata);
        data.put("script", script);
        return data;
    }

    @NotNull
    public static TileRenderRequested extractTileRenderRequestedEvent(OutputMessage<Object> message) {
        return (TileRenderRequested) message.getValue().getData();
    }
}
