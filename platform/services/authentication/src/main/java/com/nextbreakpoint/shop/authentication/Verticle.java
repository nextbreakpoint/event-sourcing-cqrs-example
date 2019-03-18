package com.nextbreakpoint.shop.authentication;

import com.nextbreakpoint.shop.authentication.handlers.GitHubSigninHandler;
import com.nextbreakpoint.shop.authentication.handlers.GitHubSignoutHandler;
import com.nextbreakpoint.shop.common.graphite.GraphiteManager;
import com.nextbreakpoint.shop.common.vertx.CORSHandlerFactory;
import com.nextbreakpoint.shop.common.vertx.MDCHandler;
import com.nextbreakpoint.shop.common.vertx.ResponseHelper;
import com.nextbreakpoint.shop.common.vertx.ServerUtil;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Launcher;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.LoggerFormat;
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

import static com.nextbreakpoint.shop.common.model.Headers.ACCEPT;
import static com.nextbreakpoint.shop.common.model.Headers.AUTHORIZATION;
import static com.nextbreakpoint.shop.common.model.Headers.CONTENT_TYPE;
import static com.nextbreakpoint.shop.common.model.Headers.COOKIE;
import static com.nextbreakpoint.shop.common.model.Headers.X_TRACE_ID;
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

        final String webUrl = config.getString("client_web_url");

        final String originPattern = config.getString("origin_pattern");

        final Router mainRouter = Router.router(vertx);

        mainRouter.route().handler(MDCHandler.create());
        mainRouter.route().handler(LoggerHandler.create(true, LoggerFormat.DEFAULT));
        mainRouter.route().handler(CookieHandler.create());
        mainRouter.route().handler(BodyHandler.create());

        final CorsHandler corsHandler = CORSHandlerFactory.createWithGetOnly(originPattern, asList(COOKIE, AUTHORIZATION, CONTENT_TYPE, ACCEPT, X_TRACE_ID));

        final Handler<RoutingContext> signinHandler = createSigninHandler(config, mainRouter);

        final Handler<RoutingContext> signoutHandler = createSignoutHandler(config, mainRouter);

        mainRouter.route("/*").handler(corsHandler);

        mainRouter.get("/auth/signin/*").handler(signinHandler);
        mainRouter.get("/auth/signout/*").handler(signoutHandler);

        mainRouter.options("/*").handler(ResponseHelper::sendNoContent);

        mainRouter.route().failureHandler(routingContext -> redirectOnFailure(routingContext, webUrl));

        final HttpServerOptions options = ServerUtil.makeServerOptions(config);

        server = vertx.createHttpServer(options)
                .requestHandler(mainRouter::accept)
                .listen(port);

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
