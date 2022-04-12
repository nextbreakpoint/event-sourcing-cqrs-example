package com.nextbreakpoint.blueprint.common.vertx;

import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.client.WebClient;

import java.net.MalformedURLException;
import java.net.URL;

public class WebClientFactory {
    private WebClientFactory() {}

    public static WebClient create(Vertx vertx, String serviceUrl, WebClientConfig webClientConfig) throws MalformedURLException {
        final URL url = new URL(serviceUrl);

        final JksOptions keyStoreOptions = new JksOptions()
                .setPath(webClientConfig.getKeyStorePath())
                .setPassword(webClientConfig.getKeyStoreSecret());

        final JksOptions trustStoreOptions = new JksOptions()
                .setPath(webClientConfig.getTrustStorePath())
                .setPassword(webClientConfig.getTrustStoreSecret());

        final WebClientOptions clientOptions = new WebClientOptions()
                .setLogActivity(true)
                .setFollowRedirects(true)
                .setDefaultHost(url.getHost())
                .setDefaultPort(url.getPort() == -1 ? url.getDefaultPort() : url.getPort())
                .setSsl(url.getProtocol().equalsIgnoreCase("https"))
                .setKeyStoreOptions(keyStoreOptions)
                .setTrustStoreOptions(trustStoreOptions)
                .setVerifyHost(webClientConfig.getVerifyHost());

        return WebClient.create(vertx, clientOptions);
    }

    public static WebClient create(Vertx vertx, String serviceUrl) throws MalformedURLException {
        final URL url = new URL(serviceUrl);

        final WebClientOptions clientOptions = new WebClientOptions()
                .setLogActivity(true)
                .setFollowRedirects(true)
                .setDefaultHost(url.getHost())
                .setDefaultPort(url.getPort() == -1 ? url.getDefaultPort() : url.getPort())
                .setSsl(url.getProtocol().equalsIgnoreCase("https"));

        return WebClient.create(vertx, clientOptions);
    }
}
