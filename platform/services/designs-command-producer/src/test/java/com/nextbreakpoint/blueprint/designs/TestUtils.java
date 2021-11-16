package com.nextbreakpoint.blueprint.designs;

import java.util.HashMap;
import java.util.Map;

public class TestUtils {
    private TestUtils() {}

    public static Map<String, Object> createPostData(String manifest, String metadata, String script) {
        final Map<String, Object> data = new HashMap<>();
        data.put("manifest", manifest);
        data.put("metadata", metadata);
        data.put("script", script);
        return data;
    }
}
