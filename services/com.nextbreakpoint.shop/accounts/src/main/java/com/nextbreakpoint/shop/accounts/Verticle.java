package com.nextbreakpoint.shop.accounts;

import com.nextbreakpoint.shop.common.AccessHandler;
import com.nextbreakpoint.shop.common.Failure;
import com.nextbreakpoint.shop.common.GraphiteManager;
import com.nextbreakpoint.shop.common.JDBCClientFactory;
import com.nextbreakpoint.shop.common.JWTProviderFactory;
import com.nextbreakpoint.shop.common.LiquibaseManager;
import com.nextbreakpoint.shop.common.ResponseHelper;
import com.nextbreakpoint.shop.common.CORSHandlerFactory;
import com.nextbreakpoint.shop.common.ServerUtil;
import io.vertx.core.Future;
import io.vertx.core.Launcher;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.http.HttpServer;
import io.vertx.rxjava.ext.auth.jwt.JWTAuth;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.handler.BodyHandler;
import io.vertx.rxjava.ext.web.handler.CookieHandler;
import io.vertx.rxjava.ext.web.handler.CorsHandler;
import io.vertx.rxjava.ext.web.handler.LoggerHandler;
import io.vertx.rxjava.ext.web.handler.TimeoutHandler;
import rx.Single;

import java.util.function.Consumer;

import static com.nextbreakpoint.shop.common.Authority.ADMIN;
import static com.nextbreakpoint.shop.common.Authority.GUEST;
import static com.nextbreakpoint.shop.common.Authority.PLATFORM;
import static com.nextbreakpoint.shop.common.ContentType.APPLICATION_JSON;
import static com.nextbreakpoint.shop.common.Headers.ACCEPT;
import static com.nextbreakpoint.shop.common.Headers.AUTHORIZATION;
import static com.nextbreakpoint.shop.common.Headers.CONTENT_TYPE;
import static com.nextbreakpoint.shop.common.Headers.XSRFTOKEN;
import static com.nextbreakpoint.shop.common.ServerUtil.UUID_REGEXP;
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

    private Void createServer(JsonObject config) throws Exception {
        LiquibaseManager.migrateDatabase(config);

        GraphiteManager.configureMetrics(config);

        final Integer port = config.getInteger("host_port");

        final String webUrl = config.getString("client_web_url");

        final JWTAuth jwtProvider = JWTProviderFactory.create(vertx, config);

        final Store store = new Store(JDBCClientFactory.create(vertx, config));

        final Router router = Router.router(vertx);

        router.route().handler(LoggerHandler.create());
        router.route().handler(BodyHandler.create());
        router.route().handler(CookieHandler.create());
        router.route().handler(TimeoutHandler.create(30000));

        final CorsHandler corsHandler = CORSHandlerFactory.createWithAll(webUrl, asList(AUTHORIZATION, CONTENT_TYPE, ACCEPT, XSRFTOKEN));

        router.route("/accounts/*").handler(corsHandler);

        final Consumer<RoutingContext> onAccessDenied = rc -> rc.fail(Failure.accessDenied());

        router.get("/accounts").handler(new AccessHandler(jwtProvider, onAccessDenied, asList(ADMIN, PLATFORM)));
        router.get("/accounts/me").handler(new AccessHandler(jwtProvider, onAccessDenied, asList(ADMIN, GUEST)));
        router.getWithRegex("/accounts/" + UUID_REGEXP).handler(new AccessHandler(jwtProvider, onAccessDenied, asList(ADMIN, PLATFORM)));
        router.put("/accounts/*").handler(new AccessHandler(jwtProvider, onAccessDenied, asList(ADMIN)));
        router.post("/accounts").handler(new AccessHandler(jwtProvider, onAccessDenied, asList(ADMIN, PLATFORM)));
        router.patch("/accounts/*").handler(new AccessHandler(jwtProvider, onAccessDenied, asList(ADMIN)));
        router.delete("/accounts/*").handler(new AccessHandler(jwtProvider, onAccessDenied, asList(ADMIN)));

        router.get("/accounts").produces(APPLICATION_JSON).handler(new ListAccountsHandler(store));

        router.get("/accounts/me").produces(APPLICATION_JSON).handler(new GetSelfAccountHandler(store));

        router.getWithRegex("/accounts/" + UUID_REGEXP).produces(APPLICATION_JSON).handler(new GetAccountHandler(store));

        router.post("/accounts").produces(APPLICATION_JSON).consumes(APPLICATION_JSON).handler(new CreateAccountHandler(store));

        router.deleteWithRegex("/accounts/" + UUID_REGEXP).produces(APPLICATION_JSON).handler(new DeleteAccountHandler(store));

        router.delete("/accounts").handler(new DeleteAccountsHandler(store));

        router.options("/accounts/*").handler(routingContext -> routingContext.response().setStatusCode(204).end());
        router.options("/accounts").handler(routingContext -> routingContext.response().setStatusCode(204).end());

        router.route().failureHandler(routingContext -> ResponseHelper.sendFailure(routingContext));

        server = vertx.createHttpServer(ServerUtil.makeServerOptions(config)).requestHandler(router::accept).listen(port);

        return null;
    }
}
