package com.nextbreakpoint.blueprint.designs;

import com.jayway.restassured.config.LogConfig;
import com.jayway.restassured.config.RedirectConfig;
import com.jayway.restassured.config.RestAssuredConfig;
import com.jayway.restassured.config.SSLConfig;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class TestUtils {
    private TestUtils() {}

    @NotNull
    public static RestAssuredConfig getRestAssuredConfig() {
        final SSLConfig sslConfig = new SSLConfig().allowAllHostnames().and().relaxedHTTPSValidation();
        final RedirectConfig redirectConfig = new RedirectConfig().followRedirects(false);
        final LogConfig logConfig = new LogConfig().enableLoggingOfRequestAndResponseIfValidationFails();
        return RestAssuredConfig.newConfig().redirect(redirectConfig).sslConfig(sslConfig).logConfig(logConfig);
    }

    public static Map<String, Object> createPostData(String manifest, String metadata, String script) {
        final Map<String, Object> data = new HashMap<>();
        data.put("manifest", manifest);
        data.put("metadata", metadata);
        data.put("script", script);
        return data;
    }
}
