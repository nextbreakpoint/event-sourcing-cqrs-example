package com.nextbreakpoint.blueprint.gateway;

import com.nextbreakpoint.blueprint.common.core.Environment;
import com.nextbreakpoint.blueprint.common.core.IOUtils;
import com.nextbreakpoint.blueprint.common.vertx.*;
import com.nextbreakpoint.blueprint.gateway.handlers.ProxyHandler;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.VertxOptions;
import io.vertx.core.dns.AddressResolverOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.Promise;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.http.HttpClient;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.handler.CorsHandler;
import io.vertx.rxjava.ext.web.handler.LoggerHandler;
import io.vertx.rxjava.ext.web.handler.TimeoutHandler;
import io.vertx.tracing.opentracing.OpenTracingOptions;
import rx.Completable;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;

import static com.nextbreakpoint.blueprint.common.core.Headers.*;
import static java.util.Arrays.asList;

public class Verticle extends AbstractVerticle {
    private static final Logger logger = LoggerFactory.getLogger(Verticle.class.getName());

    public static void main(String[] args) {
        try {
            final JsonObject config = loadConfig(args.length > 0 ? args[0] : "config/localhost.json");

            final VertxPrometheusOptions prometheusOptions = new VertxPrometheusOptions().setEnabled(true);

            final MicrometerMetricsOptions metricsOptions = new MicrometerMetricsOptions()
                    .setPrometheusOptions(prometheusOptions).setEnabled(true);

            final OpenTracingOptions tracingOptions = new OpenTracingOptions();

            final AddressResolverOptions addressResolverOptions = new AddressResolverOptions()
                    .setCacheNegativeTimeToLive(0)
                    .setCacheMaxTimeToLive(30);

            final VertxOptions vertxOptions = new VertxOptions()
                    .setAddressResolverOptions(addressResolverOptions)
                    .setMetricsOptions(metricsOptions)
                    .setTracingOptions(tracingOptions);

            final Vertx vertx = Vertx.vertx(vertxOptions);

            vertx.deployVerticle(new Verticle(), new DeploymentOptions().setConfig(config));
        } catch (Exception e) {
            logger.error("Can't start service", e);
        }
    }

    private static JsonObject loadConfig(String configPath) throws IOException {
        final Environment environment = Environment.getDefaultEnvironment();

        try (FileInputStream stream = new FileInputStream(configPath)) {
            return new JsonObject(environment.resolve(IOUtils.toString(stream)));
        }
    }

    @Override
    public Completable rxStart() {
        return vertx.rxExecuteBlocking(this::initServer)
                .doOnError(err -> logger.error("Failed to start server", err))
                .toCompletable();
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

            final Router mainRouter = Router.router(vertx);

            mainRouter.route().handler(MDCHandler.create());
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

            final ServerConfig serverConfig = ServerConfig.builder()
                    .withJksStorePath(jksStorePath)
                    .withJksStoreSecret(jksStoreSecret)
                    .build();

            final HttpServerOptions options = Server.makeOptions(serverConfig);

            vertx.createHttpServer(options)
                    .requestHandler(mainRouter)
                    .rxListen(port)
                    .doOnSuccess(result -> logger.info("Service listening on port " + port))
                    .doOnError(err -> logger.error("Can't create server", err))
                    .subscribe(result -> promise.complete(), promise::fail);
        } catch (Exception e) {
            logger.error("Failed to start server", e);
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

        final CorsHandler corsHandler = CorsHandlerFactory.createWithAll(originPattern, asList(COOKIE, AUTHORIZATION, CONTENT_TYPE, ACCEPT, X_XSRF_TOKEN, LOCATION), asList(COOKIE, CONTENT_TYPE, X_XSRF_TOKEN, LOCATION));

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
