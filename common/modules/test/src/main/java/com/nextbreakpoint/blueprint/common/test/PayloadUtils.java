package com.nextbreakpoint.blueprint.common.test;

import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.MessagePayload;
import com.nextbreakpoint.blueprint.common.vertx.Codec;
import org.apache.avro.specific.SpecificRecord;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PayloadUtils {
    private PayloadUtils() {}

    public static <T extends SpecificRecord> MessagePayload<T> mapToPayload(Map<String, Object> value, Class<T> clazz) {
        UUID uuid = UUID.fromString((String) value.get("uuid"));
        String type = (String) value.get("type");
        String source = (String) value.get("source");
        Map data = (Map) value.get("data");
        return MessagePayload.<T>builder()
                .withUuid(uuid)
                .withType(type)
                .withSource(source)
                .withData(Codec.fromString(clazz, Json.encodeValue(data)))
                .build();
    }

    public static Map<String, Object> payloadToMap(MessagePayload<? extends SpecificRecord> payload) {
        final UUID uuid = payload.getUuid();
        final String type = payload.getType();
        final String source = payload.getSource();
        final SpecificRecord record = payload.getData();
        final Class<? extends SpecificRecord> clazz = record.getClass();
        Map<String, Object> result = new HashMap<>();
        result.put("uuid", uuid.toString());
        result.put("data", Json.decodeValue(Codec.asString(clazz, record), Map.class));
        result.put("type", type);
        result.put("source", source);
        return result;
    }
}
