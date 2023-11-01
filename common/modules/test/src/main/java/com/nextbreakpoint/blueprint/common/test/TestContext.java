package com.nextbreakpoint.blueprint.common.test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class TestContext {
    private final Map<String, Object> objects = new HashMap<>();

    public void putObject(String key, Object value) {
        objects.put(key, value);
    }

    public Object getObject(String key) {
        return Optional.ofNullable(objects.get(key)).orElseThrow();
    }

    public Object getObject(String key, Object defaultValue) {
        return Optional.ofNullable(objects.get(key)).orElse(defaultValue);
    }

    public void clear() {
        objects.clear();
    }
}
