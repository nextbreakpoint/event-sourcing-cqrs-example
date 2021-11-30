package com.nextbreakpoint.blueprint.common.vertx;

import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.http.HttpClient;

import java.net.MalformedURLException;
import java.net.URL;

public class HttpClientFactory {
    private HttpClientFactory() {}

    public static HttpClient create(Vertx vertx, String serviceUrl, JsonObject config) throws MalformedURLException {
        final Boolean clientKeepAlive = Boolean.parseBoolean(config.getString("client_keep_alive"));
        final Boolean verifyHost = Boolean.parseBoolean(config.getString("client_verify_host"));
        final String clientKeyStorePath = config.getString("client_keystore_path");
        final String clientKeyStoreSecret = config.getString("client_keystore_secret");
        final String clientTrustStorePath = config.getString("client_truststore_path");
        final String clientTrustStoreSecret = config.getString("client_truststore_secret");

        final URL url = new URL(serviceUrl);

        final HttpClientOptions clientOptions = new HttpClientOptions();
        clientOptions.setKeepAlive(clientKeepAlive);
        clientOptions.setLogActivity(true);
        clientOptions.setVerifyHost(verifyHost);
        clientOptions.setDefaultHost(url.getHost());
        clientOptions.setDefaultPort(url.getPort() == -1 ? url.getDefaultPort() : url.getPort());
        clientOptions.setSsl(url.getProtocol().equalsIgnoreCase("https"));
        clientOptions.setKeyStoreOptions(new JksOptions().setPath(clientKeyStorePath).setPassword(clientKeyStoreSecret));
        clientOptions.setTrustStoreOptions(new JksOptions().setPath(clientTrustStorePath).setPassword(clientTrustStoreSecret));

        return vertx.createHttpClient(clientOptions);
    }
}
