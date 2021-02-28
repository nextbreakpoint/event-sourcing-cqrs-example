package com.nextbreakpoint.blueprint.common.vertx;

import com.nextbreakpoint.blueprint.common.core.Environment;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.client.WebClient;

import java.net.MalformedURLException;
import java.net.URL;

public class WebClientFactory {
    private WebClientFactory() {}

    public static WebClient create(Environment environment, Vertx vertx, String serviceUrl, JsonObject config) throws MalformedURLException {
        final Boolean verifyHost = Boolean.parseBoolean(environment.resolve(config.getString("client_verify_host")));
        final String clientKeyStorePath = environment.resolve(config.getString("client_keystore_path"));
        final String clientKeyStoreSecret = environment.resolve(config.getString("client_keystore_secret"));
        final String clientTrustStorePath = environment.resolve(config.getString("client_truststore_path"));
        final String clientTrustStoreSecret = environment.resolve(config.getString("client_truststore_secret"));

        final URL url = new URL(serviceUrl);

        final WebClientOptions clientOptions = new WebClientOptions();
        clientOptions.setLogActivity(true);
        clientOptions.setVerifyHost(verifyHost);
        clientOptions.setFollowRedirects(true);
        clientOptions.setDefaultHost(url.getHost());
        clientOptions.setDefaultPort(url.getPort() == -1 ? url.getDefaultPort() : url.getPort());
        clientOptions.setSsl(url.getProtocol().equalsIgnoreCase("https"));
        clientOptions.setKeyStoreOptions(new JksOptions().setPath(clientKeyStorePath).setPassword(clientKeyStoreSecret));
        clientOptions.setTrustStoreOptions(new JksOptions().setPath(clientTrustStorePath).setPassword(clientTrustStoreSecret));

        return WebClient.create(vertx, clientOptions);
    }

    public static WebClient create(Environment environment, Vertx vertx, String serviceUrl) throws MalformedURLException {
        final URL url = new URL(serviceUrl);

        final WebClientOptions clientOptions = new WebClientOptions();
        clientOptions.setLogActivity(true);
        clientOptions.setFollowRedirects(true);
        clientOptions.setDefaultHost(url.getHost());
        clientOptions.setDefaultPort(url.getPort() == -1 ? url.getDefaultPort() : url.getPort());
        clientOptions.setSsl(url.getProtocol().equalsIgnoreCase("https"));

        return WebClient.create(vertx, clientOptions);
    }
}
