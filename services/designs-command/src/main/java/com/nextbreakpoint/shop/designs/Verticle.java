package com.nextbreakpoint.shop.designs;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.nextbreakpoint.shop.common.AccessHandler;
import com.nextbreakpoint.shop.common.CORSHandlerFactory;
import com.nextbreakpoint.shop.common.CassandraClusterFactory;
import com.nextbreakpoint.shop.common.ContentHandler;
import com.nextbreakpoint.shop.common.DelegateHandler;
import com.nextbreakpoint.shop.common.FailedRequestHandler;
import com.nextbreakpoint.shop.common.Failure;
import com.nextbreakpoint.shop.common.GraphiteManager;
import com.nextbreakpoint.shop.common.JWTProviderFactory;
import com.nextbreakpoint.shop.common.NoContentHandler;
import com.nextbreakpoint.shop.common.ResponseHelper;
import com.nextbreakpoint.shop.common.ServerUtil;
import com.nextbreakpoint.shop.designs.delete.DeleteDesignController;
import com.nextbreakpoint.shop.designs.delete.DeleteDesignRequest;
import com.nextbreakpoint.shop.designs.delete.DeleteDesignRequestMapper;
import com.nextbreakpoint.shop.designs.delete.DeleteDesignResponse;
import com.nextbreakpoint.shop.designs.delete.DeleteDesignResponseMapper;
import com.nextbreakpoint.shop.designs.delete.DeleteDesignsController;
import com.nextbreakpoint.shop.designs.delete.DeleteDesignsRequest;
import com.nextbreakpoint.shop.designs.delete.DeleteDesignsRequestMapper;
import com.nextbreakpoint.shop.designs.delete.DeleteDesignsResponse;
import com.nextbreakpoint.shop.designs.delete.DeleteDesignsResponseMapper;
import com.nextbreakpoint.shop.designs.insert.InsertDesignController;
import com.nextbreakpoint.shop.designs.insert.InsertDesignRequest;
import com.nextbreakpoint.shop.designs.insert.InsertDesignRequestMapper;
import com.nextbreakpoint.shop.designs.insert.InsertDesignResponse;
import com.nextbreakpoint.shop.designs.insert.InsertDesignResponseMapper;
import com.nextbreakpoint.shop.designs.update.UpdateDesignController;
import com.nextbreakpoint.shop.designs.update.UpdateDesignRequest;
import com.nextbreakpoint.shop.designs.update.UpdateDesignRequestMapper;
import com.nextbreakpoint.shop.designs.update.UpdateDesignResponse;
import com.nextbreakpoint.shop.designs.update.UpdateDesignResponseMapper;
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

import static com.nextbreakpoint.shop.common.Authority.ADMIN;
import static com.nextbreakpoint.shop.common.ContentType.APPLICATION_JSON;
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
//        CassandraMigrationManager.migrateDatabase(config);

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

        final CorsHandler corsHandler = CORSHandlerFactory.createWithAll(webUrl, asList(AUTHORIZATION, CONTENT_TYPE, ACCEPT, XSRFTOKEN, MODIFIED), asList(CONTENT_TYPE, XSRFTOKEN, MODIFIED));

        apiRouter.route("/designs/*").handler(corsHandler);

        final Handler<RoutingContext> onAccessDenied = rc -> rc.fail(Failure.accessDenied("Authorisation failed"));

        final Handler insertDesignHandler = new AccessHandler(jwtProvider, createInsertDesignHandler(store), onAccessDenied, asList(ADMIN));

        final Handler updateDesignHandler = new AccessHandler(jwtProvider, createUpdateDesignHandler(store), onAccessDenied, asList(ADMIN));

        final Handler deleteDesignHandler = new AccessHandler(jwtProvider, createDeleteDesignHandler(store), onAccessDenied, asList(ADMIN));

        final Handler deleteDesignsHandler = new AccessHandler(jwtProvider, createDeleteDesignsHandler(store), onAccessDenied, asList(ADMIN));

        apiRouter.post("/designs")
                .produces(APPLICATION_JSON)
                .consumes(APPLICATION_JSON)
                .handler(insertDesignHandler);

        apiRouter.putWithRegex("/designs/" + UUID_REGEXP)
                .produces(APPLICATION_JSON)
                .consumes(APPLICATION_JSON)
                .handler(updateDesignHandler);

        apiRouter.deleteWithRegex("/designs/" + UUID_REGEXP)
                .produces(APPLICATION_JSON)
                .handler(deleteDesignHandler);

        apiRouter.delete("/designs")
                .handler(deleteDesignsHandler);

        apiRouter.options("/designs/*")
                .handler(ResponseHelper::sendNoContent);

        apiRouter.options("/designs")
                .handler(ResponseHelper::sendNoContent);

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

    private DelegateHandler<DeleteDesignRequest, DeleteDesignResponse> createDeleteDesignHandler(Store store) {
        return DelegateHandler.<DeleteDesignRequest, DeleteDesignResponse>builder()
                .with(new DeleteDesignRequestMapper())
                .with(new DeleteDesignResponseMapper())
                .with(new DeleteDesignController(store))
                .with(new ContentHandler(200))
                .with(new FailedRequestHandler())
                .build();
    }

    private DelegateHandler<DeleteDesignsRequest, DeleteDesignsResponse> createDeleteDesignsHandler(Store store) {
        return DelegateHandler.<DeleteDesignsRequest, DeleteDesignsResponse>builder()
                .with(new DeleteDesignsRequestMapper())
                .with(new DeleteDesignsResponseMapper())
                .with(new DeleteDesignsController(store))
                .with(new NoContentHandler(204))
                .with(new FailedRequestHandler())
                .build();
    }

    private DelegateHandler<InsertDesignRequest, InsertDesignResponse> createInsertDesignHandler(Store store) {
        return DelegateHandler.<InsertDesignRequest, InsertDesignResponse>builder()
                .with(new InsertDesignRequestMapper())
                .with(new InsertDesignResponseMapper())
                .with(new InsertDesignController(store))
                .with(new ContentHandler(201))
                .with(new FailedRequestHandler())
                .build();
    }

    private DelegateHandler<UpdateDesignRequest, UpdateDesignResponse> createUpdateDesignHandler(Store store) {
        return DelegateHandler.<UpdateDesignRequest, UpdateDesignResponse>builder()
                .with(new UpdateDesignRequestMapper())
                .with(new UpdateDesignResponseMapper())
                .with(new UpdateDesignController(store))
                .with(new ContentHandler(200))
                .with(new FailedRequestHandler())
                .build();
    }
}
