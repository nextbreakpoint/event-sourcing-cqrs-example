package com.nextbreakpoint.shop.auth;

import com.nextbreakpoint.shop.common.GraphiteManager;
import com.nextbreakpoint.shop.common.ResponseHelper;
import com.nextbreakpoint.shop.common.CORSHandlerFactory;
import com.nextbreakpoint.shop.common.ServerUtil;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Launcher;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.http.HttpServer;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.handler.BodyHandler;
import io.vertx.rxjava.ext.web.handler.CookieHandler;
import io.vertx.rxjava.ext.web.handler.CorsHandler;
import io.vertx.rxjava.ext.web.handler.LoggerHandler;
import rx.Single;

import java.net.MalformedURLException;

import static com.nextbreakpoint.shop.common.Headers.ACCEPT;
import static com.nextbreakpoint.shop.common.Headers.AUTHORIZATION;
import static com.nextbreakpoint.shop.common.Headers.CONTENT_TYPE;
import static java.util.Arrays.asList;

public class Verticle extends AbstractVerticle {
    private HttpServer server;

    public static void main(String[] args) {
        System.setProperty("vertx.metrics.options.enabled", "true");
        System.setProperty("vertx.metrics.options.registryName", "exported");

        Launcher.main(new String[] { "run", Verticle.class.getCanonicalName(), "-conf", args.length > 0 ? args[0] : "config/default.json" });
    }

    @Override
    public void start(Future<Void> startFuture) {
        final JsonObject config = vertx.getOrCreateContext().config();

        vertx.<Void>rxExecuteBlocking(future -> initServer(config, future))
                .subscribe(x -> startFuture.complete(), err -> startFuture.fail(err));
    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
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

        final String webUrl = config.getString("client_web_url");

        final Router mainRouter = Router.router(vertx);

        mainRouter.route().handler(LoggerHandler.create());
        mainRouter.route().handler(CookieHandler.create());
        mainRouter.route().handler(BodyHandler.create());

        final CorsHandler corsHandler = CORSHandlerFactory.createWithGetOnly(webUrl, asList(AUTHORIZATION, CONTENT_TYPE, ACCEPT));

        mainRouter.route("/auth/*").handler(corsHandler);

        mainRouter.get("/auth/signin/*").handler(createSigninHandler(config, mainRouter));
        mainRouter.get("/auth/signout/*").handler(createSignoutHandler(config, mainRouter));

        mainRouter.route().failureHandler(routingContext -> redirectOnFailure(routingContext, webUrl));

        server = vertx.createHttpServer(ServerUtil.makeServerOptions(config)).requestHandler(mainRouter::accept).listen(port);

        return null;
    }

    private void redirectOnFailure(RoutingContext routingContext, String webUrl) {
        ResponseHelper.redirectToError(routingContext, statusCode -> webUrl + "/error/" + statusCode);
    }

    protected Handler<RoutingContext> createSigninHandler(JsonObject config, Router router) throws MalformedURLException {
        return new GitHubSigninHandler(vertx, config, router);
    }

    protected Handler<RoutingContext> createSignoutHandler(JsonObject config, Router router) throws MalformedURLException {
        return new GitHubSignoutHandler(vertx, config, router);
    }
}
