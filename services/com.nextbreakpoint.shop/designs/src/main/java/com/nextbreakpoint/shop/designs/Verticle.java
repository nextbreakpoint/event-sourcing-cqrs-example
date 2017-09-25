package com.nextbreakpoint.shop.designs;

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
import io.vertx.rxjava.core.WorkerExecutor;
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
import static com.nextbreakpoint.shop.common.Authority.ANONYMOUS;
import static com.nextbreakpoint.shop.common.Authority.GUEST;
import static com.nextbreakpoint.shop.common.ContentType.APPLICATION_JSON;
import static com.nextbreakpoint.shop.common.ContentType.IMAGE_PNG;
import static com.nextbreakpoint.shop.common.Headers.ACCEPT;
import static com.nextbreakpoint.shop.common.Headers.AUTHORIZATION;
import static com.nextbreakpoint.shop.common.Headers.CONTENT_TYPE;
import static com.nextbreakpoint.shop.common.Headers.MODIFIED;
import static com.nextbreakpoint.shop.common.Headers.XSRFTOKEN;
import static com.nextbreakpoint.shop.common.ServerUtil.UUID_REGEXP;
import static java.util.Arrays.asList;

public class Verticle extends AbstractVerticle {
    private WorkerExecutor executor;

    private HttpServer server;

    public static void main(String[] args) {
        System.setProperty("vertx.metrics.options.enabled", "true");
        System.setProperty("vertx.metrics.options.registryName", "exported");

        Launcher.main(new String[] { "run", Verticle.class.getCanonicalName(), "-conf", args.length > 0 ? args[0] : "config/default.json" });
    }

    @Override
    public void start(Future<Void> startFuture) {
        final JsonObject config = vertx.getOrCreateContext().config();

        executor = createWorkerExecutor(config);

        vertx.<Void>rxExecuteBlocking(future -> initServer(config, future))
                .subscribe(x -> startFuture.complete(), err -> startFuture.fail(err));
    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
        if (executor != null) {
            executor.close();
        }

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

        final CorsHandler corsHandler = CORSHandlerFactory.createWithAll(webUrl, asList(AUTHORIZATION, CONTENT_TYPE, ACCEPT, XSRFTOKEN, MODIFIED), asList(CONTENT_TYPE, XSRFTOKEN, MODIFIED));

        router.route("/designs/*").handler(corsHandler);

        final Consumer<RoutingContext> onAccessDenied = rc -> rc.fail(Failure.accessDenied());

        router.get("/designs/*").handler(new AccessHandler(jwtProvider, onAccessDenied, asList(ADMIN, GUEST, ANONYMOUS)));
        router.put("/designs/*").handler(new AccessHandler(jwtProvider, onAccessDenied, asList(ADMIN)));
        router.post("/designs").handler(new AccessHandler(jwtProvider, onAccessDenied, asList(ADMIN)));
        router.patch("/designs/*").handler(new AccessHandler(jwtProvider, onAccessDenied, asList(ADMIN)));
        router.delete("/designs/*").handler(new AccessHandler(jwtProvider, onAccessDenied, asList(ADMIN)));

        router.get("/designs/:uuid/:zoom/:x/:y/:size.png").produces(IMAGE_PNG).handler(new GetTileHandler(store, executor));

        router.get("/designs").produces(APPLICATION_JSON).handler(new ListDesignsHandler(store));

        router.getWithRegex("/designs/" + UUID_REGEXP).produces(APPLICATION_JSON).handler(new GetDesignHandler(store));

        router.post("/designs").produces(APPLICATION_JSON).consumes(APPLICATION_JSON).handler(new CreateDesignHandler(store));
        router.putWithRegex("/designs/" + UUID_REGEXP).produces(APPLICATION_JSON).consumes(APPLICATION_JSON).handler(new UpdateDesignHandler(store));

        router.deleteWithRegex("/designs/" + UUID_REGEXP).produces(APPLICATION_JSON).handler(new DeleteDesignHandler(store));

        router.delete("/designs").handler(new DeleteDesignsHandler(store));

        router.options("/designs/*").handler(routingContext -> routingContext.response().setStatusCode(204).end());
        router.options("/designs").handler(routingContext -> routingContext.response().setStatusCode(204).end());

        router.get("/designs/state").produces(APPLICATION_JSON).consumes(APPLICATION_JSON).handler(new DesignStateHandler(store));
        router.get("/designs/state").produces(APPLICATION_JSON).handler(new DesignsStateHandler(store));

        router.route().failureHandler(routingContext -> ResponseHelper.sendFailure(routingContext));

        server = vertx.createHttpServer(ServerUtil.makeServerOptions(config)).requestHandler(router::accept).listen(port);

        return null;
    }

    private WorkerExecutor createWorkerExecutor(JsonObject config) {
        final int poolSize = Runtime.getRuntime().availableProcessors();
        final long maxExecuteTime = config.getInteger("max_execution_time_in_millis", 2000) * 1000000L;
        return vertx.createSharedWorkerExecutor("worker", poolSize, maxExecuteTime);
    }
}
