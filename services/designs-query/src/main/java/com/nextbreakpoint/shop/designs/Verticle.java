package com.nextbreakpoint.shop.designs;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.nextbreakpoint.shop.common.vertx.AccessHandler;
import com.nextbreakpoint.shop.common.vertx.CORSHandlerFactory;
import com.nextbreakpoint.shop.common.cassandra.CassandraClusterFactory;
import com.nextbreakpoint.shop.common.model.Failure;
import com.nextbreakpoint.shop.common.graphite.GraphiteManager;
import com.nextbreakpoint.shop.common.vertx.JWTProviderFactory;
import com.nextbreakpoint.shop.common.vertx.ResponseHelper;
import com.nextbreakpoint.shop.common.vertx.ServerUtil;
import com.nextbreakpoint.shop.designs.controllers.get.GetTileHandler;
import com.nextbreakpoint.shop.designs.persistence.CassandraStore;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Launcher;
import io.vertx.core.http.HttpServerOptions;
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

import static com.nextbreakpoint.shop.common.model.Authority.ADMIN;
import static com.nextbreakpoint.shop.common.model.Authority.ANONYMOUS;
import static com.nextbreakpoint.shop.common.model.Authority.GUEST;
import static com.nextbreakpoint.shop.common.model.ContentType.APPLICATION_JSON;
import static com.nextbreakpoint.shop.common.model.ContentType.IMAGE_PNG;
import static com.nextbreakpoint.shop.common.model.Headers.ACCEPT;
import static com.nextbreakpoint.shop.common.model.Headers.AUTHORIZATION;
import static com.nextbreakpoint.shop.common.model.Headers.CONTENT_TYPE;
import static com.nextbreakpoint.shop.common.model.Headers.X_MODIFIED;
import static com.nextbreakpoint.shop.common.model.Headers.X_XSRF_TOKEN;
import static com.nextbreakpoint.shop.common.vertx.ServerUtil.UUID_REGEXP;
import static com.nextbreakpoint.shop.designs.Factory.createListDesignsHandler;
import static com.nextbreakpoint.shop.designs.Factory.createLoadDesignHandler;
import static java.util.Arrays.asList;

public class Verticle extends AbstractVerticle {
    private WorkerExecutor executor;

    private HttpServer server;

    public static void main(String[] args) {
        System.setProperty("vertx.graphite.options.enabled", "true");
        System.setProperty("vertx.graphite.options.registryName", "exported");

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
    public void stop(Future<Void> stopFuture) {
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

    private Void createServer(JsonObject config) {
        GraphiteManager.configureMetrics(config);

        final Integer port = config.getInteger("host_port");

        final String webUrl = config.getString("client_web_url");

        final String keyspace = config.getString("cassandra_keyspace");

        final JWTAuth jwtProvider = JWTProviderFactory.create(vertx, config);

        final Cluster cluster = CassandraClusterFactory.create(config);

        final Session session = cluster.connect(keyspace);

        final Store store = new CassandraStore(session);

        final Router mainRouter = Router.router(vertx);

        final Router apiRouter = Router.router(vertx);

        mainRouter.route().handler(LoggerHandler.create());
        mainRouter.route().handler(BodyHandler.create());
        mainRouter.route().handler(CookieHandler.create());
        mainRouter.route().handler(TimeoutHandler.create(30000));

        final CorsHandler corsHandler = CORSHandlerFactory.createWithAll(webUrl, asList(AUTHORIZATION, CONTENT_TYPE, ACCEPT, X_XSRF_TOKEN, X_MODIFIED), asList(CONTENT_TYPE, X_XSRF_TOKEN, X_MODIFIED));

        apiRouter.route("/designs/*").handler(corsHandler);

        final Handler<RoutingContext> onAccessDenied = rc -> rc.fail(Failure.accessDenied("Authorisation failed"));

        final Handler getTileHandler = new AccessHandler(jwtProvider, new GetTileHandler(store, executor), onAccessDenied, asList(ADMIN, GUEST, ANONYMOUS));

        final Handler listDesignsHandler = new AccessHandler(jwtProvider, createListDesignsHandler(store), onAccessDenied, asList(ADMIN, GUEST, ANONYMOUS));

        final Handler loadDesignHandler = new AccessHandler(jwtProvider, createLoadDesignHandler(store), onAccessDenied, asList(ADMIN, GUEST, ANONYMOUS));

        apiRouter.get("/designs/:uuid/:zoom/:x/:y/:size.png")
                .produces(IMAGE_PNG)
                .handler(getTileHandler);

        apiRouter.getWithRegex("/designs/" + UUID_REGEXP)
                .produces(APPLICATION_JSON)
                .handler(loadDesignHandler);

        apiRouter.get("/designs")
                .produces(APPLICATION_JSON)
                .handler(listDesignsHandler);

        apiRouter.options("/designs/*")
                .handler(ResponseHelper::sendNoContent);

//        apiRouter.options("/designs")
//                .handler(ResponseHelper::sendNoContent);

        mainRouter.route().failureHandler(ResponseHelper::sendFailure);

        mainRouter.mountSubRouter("/api", apiRouter);

        final HttpServerOptions options = ServerUtil.makeServerOptions(config);

        server = vertx.createHttpServer(options)
                .requestHandler(mainRouter::accept)
                .listen(port);

        return null;
    }

    private WorkerExecutor createWorkerExecutor(JsonObject config) {
        final int poolSize = Runtime.getRuntime().availableProcessors();
        final long maxExecuteTime = config.getInteger("max_execution_time_in_millis", 2000) * 1000000L;
        return vertx.createSharedWorkerExecutor("worker", poolSize, maxExecuteTime);
    }
}
