package com.nextbreakpoint.shop.accounts;

import com.nextbreakpoint.shop.accounts.persistence.MySQLStore;
import com.nextbreakpoint.shop.common.graphite.GraphiteManager;
import com.nextbreakpoint.shop.common.model.Failure;
import com.nextbreakpoint.shop.common.vertx.AccessHandler;
import com.nextbreakpoint.shop.common.vertx.CORSHandlerFactory;
import com.nextbreakpoint.shop.common.vertx.JDBCClientFactory;
import com.nextbreakpoint.shop.common.vertx.JWTProviderFactory;
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
import io.vertx.rxjava.ext.auth.jwt.JWTAuth;
import io.vertx.rxjava.ext.jdbc.JDBCClient;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.handler.BodyHandler;
import io.vertx.rxjava.ext.web.handler.CookieHandler;
import io.vertx.rxjava.ext.web.handler.CorsHandler;
import io.vertx.rxjava.ext.web.handler.LoggerHandler;
import io.vertx.rxjava.ext.web.handler.TimeoutHandler;
import rx.Single;

import static com.nextbreakpoint.shop.accounts.Factory.createDeleteAccountHandler;
import static com.nextbreakpoint.shop.accounts.Factory.createInsertAccountHandler;
import static com.nextbreakpoint.shop.accounts.Factory.createListAccountsHandler;
import static com.nextbreakpoint.shop.accounts.Factory.createLoadAccountHandler;
import static com.nextbreakpoint.shop.accounts.Factory.createLoadSelfAccountHandler;
import static com.nextbreakpoint.shop.common.model.Authority.ADMIN;
import static com.nextbreakpoint.shop.common.model.Authority.GUEST;
import static com.nextbreakpoint.shop.common.model.Authority.PLATFORM;
import static com.nextbreakpoint.shop.common.model.ContentType.APPLICATION_JSON;
import static com.nextbreakpoint.shop.common.model.Headers.ACCEPT;
import static com.nextbreakpoint.shop.common.model.Headers.AUTHORIZATION;
import static com.nextbreakpoint.shop.common.model.Headers.CONTENT_TYPE;
import static com.nextbreakpoint.shop.common.model.Headers.COOKIE;
import static com.nextbreakpoint.shop.common.model.Headers.X_XSRF_TOKEN;
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

    private Void createServer(JsonObject config) {
        GraphiteManager.configureMetrics(config);

        final Integer port = config.getInteger("host_port");

        final String originPattern = config.getString("origin_pattern");

        final JWTAuth jwtProvider = JWTProviderFactory.create(vertx, config);

        final JDBCClient jdbcClient = JDBCClientFactory.create(vertx, config);

        final Store store = new MySQLStore(jdbcClient);

        final Router mainRouter = Router.router(vertx);

        mainRouter.route().handler(MDCHandler.create());
        mainRouter.route().handler(LoggerHandler.create(true, LoggerFormat.DEFAULT));
        mainRouter.route().handler(BodyHandler.create());
        mainRouter.route().handler(CookieHandler.create());
        mainRouter.route().handler(TimeoutHandler.create(30000));

        final CorsHandler corsHandler = CORSHandlerFactory.createWithAll(originPattern, asList(COOKIE, AUTHORIZATION, CONTENT_TYPE, ACCEPT, X_XSRF_TOKEN));

        mainRouter.route("/accounts/*").handler(corsHandler);

        final Handler<RoutingContext> onAccessDenied = routingContext -> routingContext.fail(Failure.accessDenied("Authentication failed"));

        final Handler<RoutingContext> listAccountsHandler = new AccessHandler(jwtProvider, createListAccountsHandler(store), onAccessDenied, asList(ADMIN, PLATFORM));

        final Handler<RoutingContext> loadSelfAccountHandler = new AccessHandler(jwtProvider, createLoadSelfAccountHandler(store), onAccessDenied, asList(ADMIN, GUEST));

        final Handler<RoutingContext> loadAccountHandler = new AccessHandler(jwtProvider, createLoadAccountHandler(store), onAccessDenied, asList(ADMIN, PLATFORM));

        final Handler<RoutingContext> insertAccountHandler = new AccessHandler(jwtProvider, createInsertAccountHandler(store), onAccessDenied, asList(ADMIN, PLATFORM));

        final Handler<RoutingContext> deleteAccountHandler = new AccessHandler(jwtProvider, createDeleteAccountHandler(store), onAccessDenied, asList(ADMIN));

        mainRouter.get("/accounts")
                .produces(APPLICATION_JSON)
                .handler(listAccountsHandler);

        mainRouter.get("/accounts/me")
                .produces(APPLICATION_JSON)
                .handler(loadSelfAccountHandler);

        mainRouter.getWithRegex("/accounts/" + UUID_REGEXP)
                .produces(APPLICATION_JSON)
                .handler(loadAccountHandler);

        mainRouter.post("/accounts")
                .produces(APPLICATION_JSON)
                .consumes(APPLICATION_JSON)
                .handler(insertAccountHandler);

        mainRouter.deleteWithRegex("/accounts/" + UUID_REGEXP)
                .produces(APPLICATION_JSON)
                .handler(deleteAccountHandler);

        mainRouter.options("/accounts/*")
                .handler(ResponseHelper::sendNoContent);

        mainRouter.route().failureHandler(ResponseHelper::sendFailure);

        final HttpServerOptions options = ServerUtil.makeServerOptions(config);

        server = vertx.createHttpServer(options)
                .requestHandler(mainRouter)
                .listen(port);

        return null;
    }
}
