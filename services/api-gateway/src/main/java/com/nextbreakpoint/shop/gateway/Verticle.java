package com.nextbreakpoint.shop.gateway;

import com.nextbreakpoint.shop.common.graphite.GraphiteManager;
import com.nextbreakpoint.shop.common.vertx.CORSHandlerFactory;
import com.nextbreakpoint.shop.common.vertx.MDCHandler;
import com.nextbreakpoint.shop.common.vertx.HttpClientFactory;
import com.nextbreakpoint.shop.common.vertx.ResponseHelper;
import com.nextbreakpoint.shop.common.vertx.ServerUtil;
import com.nextbreakpoint.shop.common.vertx.TraceHandler;
import com.nextbreakpoint.shop.gateway.handlers.ProxyHandler;
import com.nextbreakpoint.shop.gateway.handlers.WatchHandler;
import io.vertx.core.Future;
import io.vertx.core.Launcher;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.http.HttpClient;
import io.vertx.rxjava.core.http.HttpServer;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.handler.CorsHandler;
import io.vertx.rxjava.ext.web.handler.LoggerHandler;
import io.vertx.rxjava.ext.web.handler.TimeoutHandler;
import io.vertx.rxjava.servicediscovery.ServiceDiscovery;
import io.vertx.rxjava.servicediscovery.spi.ServiceImporter;
import io.vertx.servicediscovery.consul.ConsulServiceImporter;
import rx.Single;

import java.net.MalformedURLException;

import static com.nextbreakpoint.shop.common.model.Headers.*;
import static com.nextbreakpoint.shop.common.vertx.ServerUtil.UUID_REGEXP;
import static java.util.Arrays.asList;

public class Verticle extends AbstractVerticle {
    private HttpServer server;

    public static void main(String[] args) {
        System.setProperty("vertx.graphite.options.enabled", "true");
        System.setProperty("vertx.graphite.options.registryName", "exported");

        Launcher.main(new String[] { "run", Verticle.class.getCanonicalName(), "-conf", args.length > 0 ? args[0] : "config/default.json" });
    }

    @Override
    public void start(Future<Void> startFuture) {
        final JsonObject config = vertx.getOrCreateContext().config();

        vertx.<Void>rxExecuteBlocking(future -> initServer(config, future))
                .subscribe(x -> startFuture.complete(), err -> startFuture.fail(err));
    }

    @Override
    public void stop(Future<Void> stopFuture) {
        if (server != null) {
            server.rxClose().subscribe(x -> stopFuture.complete(), err -> stopFuture.fail(err));
        } else {
            stopFuture.complete();
        }
    }

    private void initServer(JsonObject config, io.vertx.rxjava.core.Future<Void> future) {
        Single.fromCallable(() -> createServer(config)).subscribe(x -> future.complete(), err -> future.fail(err));
    }

    private Void createServer(JsonObject config) throws MalformedURLException {
        GraphiteManager.configureMetrics(config);

        final Integer port = config.getInteger("host_port");

        final String originPattern = config.getString("origin_pattern");

        final Integer consulHost = config.getInteger("consul_host");
        final Integer consulPort = config.getInteger("consul_port");

        final JsonObject configuration = new JsonObject()
                .put("host", consulHost)
                .put("port", consulPort)
                .put("scan-period", 2000);

        final ServiceDiscovery serviceDiscovery = ServiceDiscovery.create(vertx);

        serviceDiscovery.registerServiceImporter(new ServiceImporter(new ConsulServiceImporter()), configuration);

        final Router mainRouter = Router.router(vertx);

        mainRouter.route().handler(TraceHandler.create());
        mainRouter.route().handler(MDCHandler.create());
        mainRouter.route().handler(LoggerHandler.create(true, LoggerFormat.DEFAULT));
        mainRouter.route().handler(TimeoutHandler.create(30000L));

        configureWatchRoute(config, mainRouter, originPattern, serviceDiscovery);

        configureAuthRoute(config, mainRouter);

        configureAccountRoute(config, mainRouter);

        configureDesignsRoute(config, mainRouter);

        final HttpServerOptions options = ServerUtil.makeServerOptions(config);

        server = vertx.createHttpServer(options)
                .requestHandler(mainRouter::accept)
                .listen(port);

        return null;
    }

    private void configureAuthRoute(JsonObject config, Router mainRouter) throws MalformedURLException {
        final Router authRouter = Router.router(vertx);

        final HttpClient authClient = HttpClientFactory.create(vertx, config.getString("server_auth_url"), config);

        authRouter.route("/*")
                .method(HttpMethod.GET)
                .handler(new ProxyHandler(authClient));

        authRouter.route("/*")
                .method(HttpMethod.OPTIONS)
                .handler(new ProxyHandler(authClient));

        mainRouter.mountSubRouter("/auth", authRouter);
    }

    private void configureAccountRoute(JsonObject config, Router mainRouter) throws MalformedURLException {
        final Router accountsRouter = Router.router(vertx);

        final HttpClient accountsClient = HttpClientFactory.create(vertx, config.getString("server_accounts_url"), config);

        accountsRouter.route("/*")
                .method(HttpMethod.GET)
                .handler(new ProxyHandler(accountsClient));

        accountsRouter.route("/*")
                .method(HttpMethod.OPTIONS)
                .handler(new ProxyHandler(accountsClient));

        accountsRouter.route("/*")
                .method(HttpMethod.POST)
                .handler(new ProxyHandler(accountsClient));

        mainRouter.mountSubRouter("/accounts", accountsRouter);
    }

    private void configureDesignsRoute(JsonObject config, Router mainRouter) throws MalformedURLException {
        final Router designsRouter = Router.router(vertx);

        final HttpClient designsCommandClient = HttpClientFactory.create(vertx, config.getString("server_designs_command_url"), config);

        final HttpClient designsQueryClient = HttpClientFactory.create(vertx, config.getString("server_designs_query_url"), config);

        designsRouter.route("/*")
                .method(HttpMethod.OPTIONS)
                .handler(new ProxyHandler(designsQueryClient));

        designsRouter.route("/*")
                .method(HttpMethod.HEAD)
                .handler(new ProxyHandler(designsQueryClient));

        designsRouter.route("/*")
                .method(HttpMethod.GET)
                .handler(new ProxyHandler(designsQueryClient));

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

        mainRouter.mountSubRouter("/designs", designsRouter);
    }

    private void configureWatchRoute(JsonObject config, Router mainRouter, String originPattern, ServiceDiscovery serviceDiscovery) {
        final Router designsRouter = Router.router(vertx);

        final CorsHandler corsHandler = CORSHandlerFactory.createWithAll(originPattern, asList(AUTHORIZATION, CONTENT_TYPE, ACCEPT, X_XSRF_TOKEN, X_MODIFIED, LOCATION), asList(CONTENT_TYPE, X_XSRF_TOKEN, X_MODIFIED, LOCATION));

        designsRouter.route("/*").handler(corsHandler);

        designsRouter.options("/*")
                .handler(ResponseHelper::sendNoContent);

        designsRouter.getWithRegex("/([0-9]+)")
                .handler(new WatchHandler(serviceDiscovery));

        designsRouter.get("/*")
                .handler(new WatchHandler(serviceDiscovery));

        mainRouter.mountSubRouter("/watch/designs", designsRouter);
    }
}
