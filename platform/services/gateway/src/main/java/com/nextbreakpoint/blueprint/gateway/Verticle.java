package com.nextbreakpoint.blueprint.gateway;

import com.nextbreakpoint.blueprint.common.vertx.*;
import com.nextbreakpoint.blueprint.gateway.handlers.ProxyHandler;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.Promise;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.http.HttpClient;
import io.vertx.rxjava.ext.healthchecks.HealthCheckHandler;
import io.vertx.rxjava.ext.healthchecks.HealthChecks;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.handler.CorsHandler;
import io.vertx.rxjava.ext.web.handler.LoggerHandler;
import io.vertx.rxjava.ext.web.handler.TimeoutHandler;
import io.vertx.rxjava.micrometer.PrometheusScrapingHandler;
import lombok.extern.log4j.Log4j2;
import rx.Completable;

import java.net.MalformedURLException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.nextbreakpoint.blueprint.common.core.Headers.*;

@Log4j2
public class Verticle extends AbstractVerticle {
    public static void main(String[] args) {
        try {
            final JsonObject config = Initializer.loadConfig(args.length > 0 ? args[0] : "config/localhost.json");

            final Vertx vertx = Initializer.createVertx();

            vertx.rxDeployVerticle(new Verticle(), new DeploymentOptions().setConfig(config))
                    .delay(30, TimeUnit.SECONDS)
                    .retry(3)
                    .subscribe(o -> log.info("Verticle deployed"), err -> log.error("Can't deploy verticle"));
        } catch (Exception e) {
            log.error("Can't start service", e);
        }
    }

    @Override
    public Completable rxStart() {
        return vertx.rxExecuteBlocking(this::initServer).toCompletable();
    }

    private void initServer(Promise<Void> promise) {
        try {
            final JsonObject config = vertx.getOrCreateContext().config();

            final int port = Integer.parseInt(config.getString("host_port"));

            final String originPattern = config.getString("origin_pattern");

            final String jksStorePath = config.getString("server_keystore_path");

            final String jksStoreSecret = config.getString("server_keystore_secret");

            final String clientKeyStorePath = config.getString("client_keystore_path");

            final String clientKeyStoreSecret = config.getString("client_keystore_secret");

            final String clientTrustStorePath = config.getString("client_truststore_path");

            final String clientTrustStoreSecret = config.getString("client_truststore_secret");

            final Boolean keepAlive = Boolean.parseBoolean(config.getString("client_keep_alive"));

            final Boolean verifyHost = Boolean.parseBoolean(config.getString("client_verify_host"));

            final String authUrl = config.getString("server_auth_url");

            final String accountsUrl = config.getString("server_accounts_url");

            final String designsCommandUrl = config.getString("server_designs_command_url");

            final String designsRenderUrl = config.getString("server_designs_render_url");

            final String designsQueryUrl = config.getString("server_designs_query_url");

            final String designsWatchUrl = config.getString("server_designs_watch_url");

            final HealthCheckHandler healthCheckHandler = HealthCheckHandler.createWithHealthChecks(HealthChecks.create(vertx));

            final Router mainRouter = Router.router(vertx);

            mainRouter.get("/health*").handler(healthCheckHandler);

            mainRouter.route().handler(LoggerHandler.create(true, LoggerFormat.DEFAULT));
            mainRouter.route().handler(TimeoutHandler.create(30000L));

            final HttpClientConfig clientConfig = HttpClientConfig.builder()
                    .withKeepAlive(keepAlive)
                    .withVerifyHost(verifyHost)
                    .withKeyStorePath(clientKeyStorePath)
                    .withKeyStoreSecret(clientKeyStoreSecret)
                    .withTrustStorePath(clientTrustStorePath)
                    .withTrustStoreSecret(clientTrustStoreSecret)
                    .build();

            configureWatchRoute(mainRouter, clientConfig, originPattern, designsWatchUrl);

            configureAuthRoute(mainRouter, clientConfig, authUrl);

            configureAccountRoute(mainRouter, clientConfig, accountsUrl);

            configureDesignsRoute(mainRouter, clientConfig, designsCommandUrl, designsRenderUrl, designsQueryUrl);

            mainRouter.route("/metrics").handler(PrometheusScrapingHandler.create());

            final ServerConfig serverConfig = ServerConfig.builder()
                    .withJksStorePath(jksStorePath)
                    .withJksStoreSecret(jksStoreSecret)
                    .build();

            final HttpServerOptions options = Server.makeOptions(serverConfig);

            vertx.createHttpServer(options)
                    .requestHandler(mainRouter)
                    .rxListen(port)
                    .doOnSuccess(result -> log.info("Service listening on port " + port))
                    .doOnError(err -> log.error("Can't create server", err))
                    .subscribe(result -> promise.complete(), promise::fail);
        } catch (Exception e) {
            log.error("Failed to start server", e);
            promise.fail(e);
        }
    }

    private void configureAuthRoute(Router mainRouter, HttpClientConfig clientConfig, String authUrl) throws MalformedURLException {
        final Router authRouter = Router.router(vertx);

        final HttpClient authClient = HttpClientFactory.create(vertx, authUrl, clientConfig);

        authRouter.route("/*")
                .method(HttpMethod.GET)
                .handler(new ProxyHandler(authClient));

        authRouter.route("/*")
                .method(HttpMethod.OPTIONS)
                .handler(new ProxyHandler(authClient));

        mainRouter.mountSubRouter("/v1/auth", authRouter);
    }

    private void configureAccountRoute(Router mainRouter, HttpClientConfig clientConfig, String accountsUrl) throws MalformedURLException {
        final Router accountsRouter = Router.router(vertx);

        final HttpClient accountsClient = HttpClientFactory.create(vertx, accountsUrl, clientConfig);

        accountsRouter.route("/*")
                .method(HttpMethod.GET)
                .handler(new ProxyHandler(accountsClient));

        accountsRouter.route("/*")
                .method(HttpMethod.OPTIONS)
                .handler(new ProxyHandler(accountsClient));

        accountsRouter.route("/*")
                .method(HttpMethod.POST)
                .handler(new ProxyHandler(accountsClient));

        mainRouter.mountSubRouter("/v1/accounts", accountsRouter);
    }

    private void configureDesignsRoute(Router mainRouter, HttpClientConfig clientConfig, String designsCommandUrl, String designsRenderUrl, String designsQueryUrl) throws MalformedURLException {
        final Router designsRouter = Router.router(vertx);

        final HttpClient designsCommandClient = HttpClientFactory.create(vertx, designsCommandUrl, clientConfig);

        final HttpClient designsRenderClient = HttpClientFactory.create(vertx, designsRenderUrl, clientConfig);

        final HttpClient designsQueryClient = HttpClientFactory.create(vertx, designsQueryUrl, clientConfig);

        designsRouter.route("/validate")
                .method(HttpMethod.POST)
                .handler(new ProxyHandler(designsRenderClient));

        designsRouter.route("/download")
                .method(HttpMethod.POST)
                .handler(new ProxyHandler(designsRenderClient));

        designsRouter.route("/upload")
                .method(HttpMethod.POST)
                .handler(new ProxyHandler(designsRenderClient));

        designsRouter.route("/*")
                .method(HttpMethod.HEAD)
                .handler(new ProxyHandler(designsQueryClient));

        designsRouter.route("/*")
                .method(HttpMethod.GET)
                .handler(new ProxyHandler(designsQueryClient));

        designsRouter.route("/*")
                .method(HttpMethod.OPTIONS)
                .handler(new ProxyHandler(designsCommandClient));

        designsRouter.route("/*")
                .method(HttpMethod.POST)
                .handler(new ProxyHandler(designsCommandClient));

        designsRouter.route("/*")
                .method(HttpMethod.PUT)
                .handler(new ProxyHandler(designsCommandClient));

        designsRouter.route("/*")
                .method(HttpMethod.PATCH)
                .handler(new ProxyHandler(designsCommandClient));

        designsRouter.route("/*")
                .method(HttpMethod.DELETE)
                .handler(new ProxyHandler(designsCommandClient));

        mainRouter.mountSubRouter("/v1/designs", designsRouter);
    }

    private void configureWatchRoute(Router mainRouter, HttpClientConfig clientConfig, String originPattern, String designsWatchUrl) throws MalformedURLException {
        final Router designsRouter = Router.router(vertx);

        final HttpClient designsWatchClient = HttpClientFactory.create(vertx, designsWatchUrl, clientConfig);

        final CorsHandler corsHandler = CorsHandlerFactory.createWithAll(originPattern, List.of(COOKIE, AUTHORIZATION, CONTENT_TYPE, ACCEPT, X_XSRF_TOKEN, LOCATION), List.of(COOKIE, CONTENT_TYPE, X_XSRF_TOKEN, LOCATION));

        designsRouter.route("/*").handler(corsHandler);

        designsRouter.get("/*")
                .method(HttpMethod.OPTIONS)
                .handler(new ProxyHandler(designsWatchClient));

        designsRouter.get("/*")
                .method(HttpMethod.GET)
                .handler(new ProxyHandler(designsWatchClient));

        mainRouter.mountSubRouter("/v1/watch/designs", designsRouter);
    }
}
