package com.nextbreakpoint.shop.gateway;

import com.nextbreakpoint.shop.common.graphite.GraphiteManager;
import com.nextbreakpoint.shop.common.vertx.CORSHandlerFactory;
import com.nextbreakpoint.shop.common.vertx.JWTProviderFactory;
import com.nextbreakpoint.shop.common.vertx.ResponseHelper;
import com.nextbreakpoint.shop.common.vertx.ServerUtil;
import com.nextbreakpoint.shop.common.vertx.WebClientFactory;
import com.nextbreakpoint.shop.gateway.handlers.UserHandler;
import io.vertx.core.Future;
import io.vertx.core.Launcher;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.http.HttpServer;
import io.vertx.rxjava.ext.auth.jwt.JWTAuth;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.client.WebClient;
import io.vertx.rxjava.ext.web.handler.BodyHandler;
import io.vertx.rxjava.ext.web.handler.CSRFHandler;
import io.vertx.rxjava.ext.web.handler.CookieHandler;
import io.vertx.rxjava.ext.web.handler.CorsHandler;
import io.vertx.rxjava.ext.web.handler.LoggerHandler;
import io.vertx.rxjava.ext.web.handler.TimeoutHandler;
import rx.Single;

import java.net.MalformedURLException;

import static com.nextbreakpoint.shop.common.model.Headers.ACCEPT;
import static com.nextbreakpoint.shop.common.model.Headers.AUTHORIZATION;
import static com.nextbreakpoint.shop.common.model.Headers.CONTENT_TYPE;
import static com.nextbreakpoint.shop.common.model.Headers.X_MODIFIED;
import static com.nextbreakpoint.shop.common.model.Headers.X_XSRF_TOKEN;
import static com.nextbreakpoint.shop.common.vertx.ServerUtil.UUID_REGEXP;
import static java.util.Arrays.asList;

public class Verticle extends AbstractVerticle {
    private static final String TEMPLATES = "templates";

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

        final String secret = config.getString("csrf_secret");

        final String originPattern = config.getString("origin_pattern");

        final JWTAuth jwtProvider = JWTProviderFactory.create(vertx, config);

        final WebClient accountsClient = WebClientFactory.create(vertx, config.getString("server_accounts_url"), config);

        final Router mainRouter = Router.router(vertx);

        mainRouter.route().handler(LoggerHandler.create());
        mainRouter.route().handler(CookieHandler.create());
        mainRouter.route().handler(BodyHandler.create());
        mainRouter.route().handler(TimeoutHandler.create(30000));

        final CorsHandler corsHandler = CORSHandlerFactory.createWithAll(originPattern, asList(AUTHORIZATION, CONTENT_TYPE, ACCEPT, X_XSRF_TOKEN, X_MODIFIED), asList(CONTENT_TYPE, X_XSRF_TOKEN, X_MODIFIED));

        mainRouter.route("/*").handler(corsHandler);

        mainRouter.route("/*").handler(CSRFHandler.create(secret));

        mainRouter.route("/*").handler(UserHandler.create(jwtProvider, accountsClient));

        mainRouter.getWithRegex("/watch/designs/([0-9]+)/" + UUID_REGEXP)
                .handler(routingContext -> ResponseHelper.redirectToURL(routingContext, () -> config.getString("client_designs_sse_url") + "/" + routingContext.request().getParam("param0") + "/" + routingContext.request().getParam("param1")));

        mainRouter.getWithRegex("/watch/designs/([0-9]+)")
                .handler(routingContext -> ResponseHelper.redirectToURL(routingContext, () -> config.getString("client_designs_sse_url") + "/" + routingContext.request().getParam("param0")));

        final HttpServerOptions options = ServerUtil.makeServerOptions(config);

        server = vertx.createHttpServer(options)
                .requestHandler(mainRouter::accept)
                .listen(port);

        return null;
    }
}
