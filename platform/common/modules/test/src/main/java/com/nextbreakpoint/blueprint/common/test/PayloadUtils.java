package com.nextbreakpoint.blueprint.common.test;

import com.nextbreakpoint.blueprint.common.core.Payload;
import com.nextbreakpoint.blueprint.common.core.Json;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PayloadUtils {
    private PayloadUtils() {}

    public static Payload mapToPayload(Map<String, Object> value) {
        String uuid = (String) value.get("uuid");
        String type = (String) value.get("type");
        String source = (String) value.get("source");
        Map data = (Map) value.get("data");
        return new Payload(UUID.fromString(uuid), type, Json.encodeValue(data), source);
    }

    public static Map<String, Object> payloadToMap(Payload payload) {
        Map<String, Object> result = new HashMap<>();
        result.put("uuid", payload.getUuid().toString());
        result.put("data", Json.decodeValue(payload.getData(), Map.class));
        result.put("type", payload.getType());
        result.put("source", payload.getSource());
        return result;
    }
}
