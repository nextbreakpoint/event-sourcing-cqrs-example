package com.nextbreakpoint.blueprint.designs;

import au.com.dius.pact.core.model.messaging.Message;
import com.nextbreakpoint.blueprint.common.core.KafkaRecord;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.core.Payload;
import com.nextbreakpoint.blueprint.common.events.TileRenderRequested;
import io.vertx.core.json.Json;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TestUtils {
    private TestUtils() {}

    @NotNull
    public static String createBucketKey(TileRenderRequested event) {
        return String.format("%s/%d/%04d%04d.png", event.getChecksum(), event.getLevel(), event.getRow(), event.getCol());
    }

    @NotNull
    public static OutputMessage toOutputMessage(Message message) {
        final KafkaRecord kafkaRecord1 = Json.decodeValue(message.contentsAsString(), KafkaRecord.class);
        return OutputMessage.from(kafkaRecord1.getKey(), mapToPayload(kafkaRecord1.getValue()));
    }

    @NotNull
    public static Payload mapToPayload(Map<String, Object> value) {
        String uuid = (String) value.get("uuid");
        String type = (String) value.get("type");
        String source = (String) value.get("source");
        Map data = (Map) value.get("data");
        return new Payload(UUID.fromString(uuid), type, Json.encode(data), source);
    }

    @NotNull
    public static Map<String, Object> payloadToMap(Payload payload) {
        Map<String, Object> result = new HashMap<>();
        result.put("uuid", payload.getUuid().toString());
        result.put("data", Json.decodeValue(payload.getData(), Map.class));
        result.put("type", payload.getType());
        result.put("source", payload.getSource());
        return result;
    }
}
