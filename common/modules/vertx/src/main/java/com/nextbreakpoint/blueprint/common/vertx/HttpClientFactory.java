package com.nextbreakpoint.blueprint.common.vertx;

import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.http.HttpClient;

import java.net.MalformedURLException;
import java.net.URL;

public class HttpClientFactory {
    private HttpClientFactory() {}

    public static HttpClient create(Vertx vertx, String serviceUrl, HttpClientConfig httpClientConfig) throws MalformedURLException {
        final URL url = new URL(serviceUrl);

        final JksOptions keyStoreOptions = new JksOptions()
                .setPath(httpClientConfig.getKeyStorePath())
                .setPassword(httpClientConfig.getKeyStoreSecret());

        final JksOptions trustStoreOptions = new JksOptions()
                .setPath(httpClientConfig.getTrustStorePath())
                .setPassword(httpClientConfig.getTrustStoreSecret());

        final HttpClientOptions clientOptions = new WebClientOptions()
                .setLogActivity(true)
                .setFollowRedirects(true)
                .setDefaultHost(url.getHost())
                .setDefaultPort(url.getPort() == -1 ? url.getDefaultPort() : url.getPort())
                .setSsl(url.getProtocol().equalsIgnoreCase("https"))
                .setKeyStoreOptions(keyStoreOptions)
                .setTrustStoreOptions(trustStoreOptions)
                .setVerifyHost(httpClientConfig.getVerifyHost())
                .setKeepAlive(httpClientConfig.getKeepAlive());

        return vertx.createHttpClient(clientOptions);
    }
}
