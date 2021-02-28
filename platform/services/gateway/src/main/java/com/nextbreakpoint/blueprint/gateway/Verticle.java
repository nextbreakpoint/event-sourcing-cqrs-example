package com.nextbreakpoint.blueprint.gateway;

import com.nextbreakpoint.blueprint.common.core.Environment;
import com.nextbreakpoint.blueprint.gateway.handlers.ProxyHandler;
import com.nextbreakpoint.blueprint.gateway.handlers.WatchHandler;
import com.nextbreakpoint.blueprint.common.vertx.CorsHandlerFactory;
import com.nextbreakpoint.blueprint.common.vertx.MDCHandler;
import com.nextbreakpoint.blueprint.common.vertx.HttpClientFactory;
import com.nextbreakpoint.blueprint.common.vertx.ResponseHelper;
import com.nextbreakpoint.blueprint.common.vertx.ServerUtil;
import com.nextbreakpoint.blueprint.common.vertx.TraceHandler;
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

import static com.nextbreakpoint.blueprint.common.core.Headers.*;
import static java.util.Arrays.asList;

public class Verticle extends AbstractVerticle {
    private HttpServer server;

    public static void main(String[] args) {
        System.setProperty("vertx.graphite.options.enabled", "true");
        System.setProperty("vertx.graphite.options.registryName", "exported");

        //java.security.Security.setProperty("networkaddress.cache.ttl", "<value>");

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
        final Environment environment = Environment.getDefaultEnvironment();

        final Integer port = Integer.parseInt(environment.resolve(config.getString("host_port")));

        final String originPattern = environment.resolve(config.getString("origin_pattern"));

        final String consulHost = environment.resolve(config.getString("consul_host"));
        final Integer consulPort = Integer.parseInt(environment.resolve(config.getString("consul_port")));

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

        configureWatchRoute(environment, config, mainRouter, originPattern, serviceDiscovery);

        configureAuthRoute(environment, config, mainRouter);

        configureAccountRoute(environment, config, mainRouter);

        configureDesignsRoute(environment, config, mainRouter);

        final HttpServerOptions options = ServerUtil.makeServerOptions(environment, config);

        server = vertx.createHttpServer(options)
                .requestHandler(mainRouter::accept)
                .listen(port);

        return null;
    }

    private void configureAuthRoute(Environment environment, JsonObject config, Router mainRouter) throws MalformedURLException {
        final Router authRouter = Router.router(vertx);

        final HttpClient authClient = HttpClientFactory.create(environment, vertx, config.getString("server_auth_url"), config);

        authRouter.route("/*")
                .method(HttpMethod.GET)
                .handler(new ProxyHandler(authClient));

        authRouter.route("/*")
                .method(HttpMethod.OPTIONS)
                .handler(new ProxyHandler(authClient));

        mainRouter.mountSubRouter("/auth", authRouter);
    }

    private void configureAccountRoute(Environment environment, JsonObject config, Router mainRouter) throws MalformedURLException {
        final Router accountsRouter = Router.router(vertx);

        final HttpClient accountsClient = HttpClientFactory.create(environment, vertx, config.getString("server_accounts_url"), config);

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

    private void configureDesignsRoute(Environment environment, JsonObject config, Router mainRouter) throws MalformedURLException {
        final Router designsRouter = Router.router(vertx);

        final HttpClient designsCommandClient = HttpClientFactory.create(environment, vertx, config.getString("server_designs_command_url"), config);

        final HttpClient designsQueryClient = HttpClientFactory.create(environment, vertx, config.getString("server_designs_query_url"), config);

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

        mainRouter.mountSubRouter("/designs", designsRouter);
    }

    private void configureWatchRoute(Environment environment, JsonObject config, Router mainRouter, String originPattern, ServiceDiscovery serviceDiscovery) {
        final Router designsRouter = Router.router(vertx);

        final CorsHandler corsHandler = CorsHandlerFactory.createWithAll(originPattern, asList(COOKIE, AUTHORIZATION, CONTENT_TYPE, ACCEPT, X_XSRF_TOKEN, X_MODIFIED, LOCATION), asList(COOKIE, CONTENT_TYPE, X_XSRF_TOKEN, X_MODIFIED, LOCATION));

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
