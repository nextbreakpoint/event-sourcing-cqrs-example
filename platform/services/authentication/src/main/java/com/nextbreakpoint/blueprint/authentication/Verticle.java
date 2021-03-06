package com.nextbreakpoint.blueprint.authentication;

import com.nextbreakpoint.blueprint.authentication.handlers.GitHubSignoutHandler;
import com.nextbreakpoint.blueprint.authentication.handlers.GitHubSigninHandler;
import com.nextbreakpoint.blueprint.common.core.Environment;
import com.nextbreakpoint.blueprint.common.vertx.CorsHandlerFactory;
import com.nextbreakpoint.blueprint.common.vertx.MDCHandler;
import com.nextbreakpoint.blueprint.common.vertx.ResponseHelper;
import com.nextbreakpoint.blueprint.common.vertx.ServerUtil;
import io.vertx.core.Handler;
import io.vertx.core.Launcher;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.Promise;
import io.vertx.rxjava.core.http.HttpServer;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.handler.BodyHandler;
import io.vertx.rxjava.ext.web.handler.CorsHandler;
import io.vertx.rxjava.ext.web.handler.LoggerHandler;
import rx.Completable;
import rx.Single;

import java.net.MalformedURLException;

import static com.nextbreakpoint.blueprint.common.core.Headers.ACCEPT;
import static com.nextbreakpoint.blueprint.common.core.Headers.AUTHORIZATION;
import static com.nextbreakpoint.blueprint.common.core.Headers.CONTENT_TYPE;
import static com.nextbreakpoint.blueprint.common.core.Headers.COOKIE;
import static com.nextbreakpoint.blueprint.common.core.Headers.X_TRACE_ID;
import static java.util.Arrays.asList;

public class Verticle extends AbstractVerticle {
    private final Logger logger = LoggerFactory.getLogger(Verticle.class.getName());

    public static void main(String[] args) {
        Launcher.main(new String[] { "run", Verticle.class.getCanonicalName(), "-conf", args.length > 0 ? args[0] : "config/localhost.json" });
    }

    @Override
    public Completable rxStart() {
        return vertx.rxExecuteBlocking(this::initServer).toCompletable();
    }

    private void initServer(Promise<Void> promise) {
        Single.fromCallable(this::createServer).subscribe(httpServer -> promise.complete(), promise::fail);
    }

    private HttpServer createServer() throws MalformedURLException {
        final JsonObject config = vertx.getOrCreateContext().config();

        final Environment environment = Environment.getDefaultEnvironment();

        final Integer port = Integer.parseInt(environment.resolve(config.getString("host_port")));

        final String webUrl = environment.resolve(config.getString("client_web_url"));

        final String originPattern = environment.resolve(config.getString("origin_pattern"));

        final Router mainRouter = Router.router(vertx);

        mainRouter.route().handler(MDCHandler.create());
        mainRouter.route().handler(LoggerHandler.create(true, LoggerFormat.DEFAULT));
//        mainRouter.route().handler(CookieHandler.create());
        mainRouter.route().handler(BodyHandler.create());

        final CorsHandler corsHandler = CorsHandlerFactory.createWithGetOnly(originPattern, asList(COOKIE, AUTHORIZATION, CONTENT_TYPE, ACCEPT, X_TRACE_ID));

        final Handler<RoutingContext> signinHandler = createSigninHandler(environment, config, mainRouter);

        final Handler<RoutingContext> signoutHandler = createSignoutHandler(environment, config, mainRouter);

        mainRouter.route("/*").handler(corsHandler);

        mainRouter.get("/auth/signin").handler(signinHandler);
        mainRouter.get("/auth/signout").handler(signoutHandler);
        mainRouter.get("/auth/signin/*").handler(signinHandler);
        mainRouter.get("/auth/signout/*").handler(signoutHandler);

        mainRouter.options("/*").handler(ResponseHelper::sendNoContent);

        mainRouter.route().failureHandler(routingContext -> redirectOnFailure(routingContext, webUrl));

        final HttpServerOptions options = ServerUtil.makeServerOptions(environment, config);

        return vertx.createHttpServer(options)
                .requestHandler(mainRouter::handle)
                .rxListen(port)
                .doOnSuccess(result -> logger.info("Service listening on port " + port))
                .toBlocking()
                .value();
    }

    private void redirectOnFailure(RoutingContext routingContext, String webUrl) {
        ResponseHelper.redirectToError(routingContext, statusCode -> webUrl + "/error/" + statusCode);
    }

    protected Handler<RoutingContext> createSigninHandler(Environment environment, JsonObject config, Router router) throws MalformedURLException {
        return new GitHubSigninHandler(environment, vertx, config, router);
    }

    protected Handler<RoutingContext> createSignoutHandler(Environment environment, JsonObject config, Router router) throws MalformedURLException {
        return new GitHubSignoutHandler(environment, vertx, config, router);
    }
}
