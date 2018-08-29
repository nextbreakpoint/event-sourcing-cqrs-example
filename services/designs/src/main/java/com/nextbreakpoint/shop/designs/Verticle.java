package com.nextbreakpoint.shop.designs;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.nextbreakpoint.shop.common.vertx.AccessHandler;
import com.nextbreakpoint.shop.common.cassandra.CassandraClusterFactory;
import com.nextbreakpoint.shop.common.model.Content;
import com.nextbreakpoint.shop.common.handlers.DefaultHandler;
import com.nextbreakpoint.shop.common.model.Failure;
import com.nextbreakpoint.shop.common.graphite.GraphiteManager;
import com.nextbreakpoint.shop.common.vertx.JWTProviderFactory;
import com.nextbreakpoint.shop.common.vertx.ResponseHelper;
import com.nextbreakpoint.shop.common.vertx.CORSHandlerFactory;
import com.nextbreakpoint.shop.common.vertx.ServerUtil;
import com.nextbreakpoint.shop.common.handlers.FailedRequestConsumer;
import com.nextbreakpoint.shop.designs.common.ContentConsumer;
import com.nextbreakpoint.shop.designs.model.DeleteDesignRequest;
import com.nextbreakpoint.shop.designs.controllers.delete.DeleteDesignController;
import com.nextbreakpoint.shop.designs.controllers.delete.DeleteDesignRequestMapper;
import com.nextbreakpoint.shop.designs.model.DeleteDesignResponse;
import com.nextbreakpoint.shop.designs.controllers.delete.DeleteDesignResponseMapper;
import com.nextbreakpoint.shop.designs.common.NoContentConsumer;
import com.nextbreakpoint.shop.designs.model.DeleteDesignsRequest;
import com.nextbreakpoint.shop.designs.controllers.delete.DeleteDesignsController;
import com.nextbreakpoint.shop.designs.controllers.delete.DeleteDesignsRequestMapper;
import com.nextbreakpoint.shop.designs.model.DeleteDesignsResponse;
import com.nextbreakpoint.shop.designs.controllers.delete.DeleteDesignsResponseMapper;
import com.nextbreakpoint.shop.designs.controllers.tile.GetTileHandler;
import com.nextbreakpoint.shop.designs.model.InsertDesignRequest;
import com.nextbreakpoint.shop.designs.controllers.insert.InsertDesignController;
import com.nextbreakpoint.shop.designs.controllers.insert.InsertDesignRequestMapper;
import com.nextbreakpoint.shop.designs.model.InsertDesignResponse;
import com.nextbreakpoint.shop.designs.controllers.insert.InsertDesignResponseMapper;
import com.nextbreakpoint.shop.designs.model.ListDesignsRequest;
import com.nextbreakpoint.shop.designs.controllers.list.ListDesignsController;
import com.nextbreakpoint.shop.designs.controllers.list.ListDesignsRequestMapper;
import com.nextbreakpoint.shop.designs.model.ListDesignsResponse;
import com.nextbreakpoint.shop.designs.controllers.list.ListDesignsResponseMapper;
import com.nextbreakpoint.shop.designs.model.LoadDesignRequest;
import com.nextbreakpoint.shop.designs.controllers.load.LoadDesignController;
import com.nextbreakpoint.shop.designs.controllers.load.LoadDesignRequestMapper;
import com.nextbreakpoint.shop.designs.model.LoadDesignResponse;
import com.nextbreakpoint.shop.designs.controllers.load.LoadDesignResponseMapper;
import com.nextbreakpoint.shop.designs.model.GetStatusRequest;
import com.nextbreakpoint.shop.designs.controllers.get.GetStatusController;
import com.nextbreakpoint.shop.designs.controllers.get.GetStatusRequestMapper;
import com.nextbreakpoint.shop.designs.model.GetStatusResponse;
import com.nextbreakpoint.shop.designs.controllers.get.GetStatusResponseMapper;
import com.nextbreakpoint.shop.designs.model.ListStatusRequest;
import com.nextbreakpoint.shop.designs.controllers.list.ListStatusController;
import com.nextbreakpoint.shop.designs.controllers.list.ListStatusRequestMapper;
import com.nextbreakpoint.shop.designs.model.ListStatusResponse;
import com.nextbreakpoint.shop.designs.controllers.list.ListStatusResponseMapper;
import com.nextbreakpoint.shop.designs.model.UpdateDesignRequest;
import com.nextbreakpoint.shop.designs.controllers.update.UpdateDesignController;
import com.nextbreakpoint.shop.designs.controllers.update.UpdateDesignRequestMapper;
import com.nextbreakpoint.shop.designs.model.UpdateDesignResponse;
import com.nextbreakpoint.shop.designs.controllers.update.UpdateDesignResponseMapper;
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

        final Handler insertDesignHandler = new AccessHandler(jwtProvider, createInsertDesignHandler(store), onAccessDenied, asList(ADMIN));

        final Handler updateDesignHandler = new AccessHandler(jwtProvider, createUpdateDesignHandler(store), onAccessDenied, asList(ADMIN));

        final Handler deleteDesignHandler = new AccessHandler(jwtProvider, createDeleteDesignHandler(store), onAccessDenied, asList(ADMIN));

        final Handler deleteDesignsHandler = new AccessHandler(jwtProvider, createDeleteDesignsHandler(store), onAccessDenied, asList(ADMIN));

        final Handler getStatusHandler = new AccessHandler(jwtProvider, createGetStatusHandler(store), onAccessDenied, asList(ADMIN, GUEST, ANONYMOUS));

        final Handler getStatusListHandler = new AccessHandler(jwtProvider, createListStatusHandler(store), onAccessDenied, asList(ADMIN, GUEST, ANONYMOUS));

        apiRouter.get("/designs/:uuid/:zoom/:x/:y/:size.png")
                .produces(IMAGE_PNG)
                .handler(getTileHandler);

        apiRouter.get("/designs")
                .produces(APPLICATION_JSON)
                .handler(listDesignsHandler);

        apiRouter.getWithRegex("/designs/" + UUID_REGEXP)
                .produces(APPLICATION_JSON)
                .handler(loadDesignHandler);

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

        apiRouter.get("/designs/status")
                .produces(APPLICATION_JSON)
                .handler(getStatusHandler);

        apiRouter.get("/designs/statusList")
                .produces(APPLICATION_JSON)
                .handler(getStatusListHandler);

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

    private DefaultHandler<RoutingContext, DeleteDesignRequest, DeleteDesignResponse, Content> createDeleteDesignHandler(Store store) {
        return DefaultHandler.<RoutingContext, DeleteDesignRequest, DeleteDesignResponse, Content>builder()
                .withInputMapper(new DeleteDesignRequestMapper())
                .withOutputMapper(new DeleteDesignResponseMapper())
                .withController(new DeleteDesignController(store))
                .onSuccess(new ContentConsumer(200))
                .onFailure(new FailedRequestConsumer())
                .build();
    }

    private DefaultHandler<RoutingContext, DeleteDesignsRequest, DeleteDesignsResponse, Content> createDeleteDesignsHandler(Store store) {
        return DefaultHandler.<RoutingContext, DeleteDesignsRequest, DeleteDesignsResponse, Content>builder()
                .withInputMapper(new DeleteDesignsRequestMapper())
                .withOutputMapper(new DeleteDesignsResponseMapper())
                .withController(new DeleteDesignsController(store))
                .onSuccess(new NoContentConsumer(204))
                .onFailure(new FailedRequestConsumer())
                .build();
    }

    private DefaultHandler<RoutingContext, InsertDesignRequest, InsertDesignResponse, Content> createInsertDesignHandler(Store store) {
        return DefaultHandler.<RoutingContext, InsertDesignRequest, InsertDesignResponse, Content>builder()
                .withInputMapper(new InsertDesignRequestMapper())
                .withOutputMapper(new InsertDesignResponseMapper())
                .withController(new InsertDesignController(store))
                .onSuccess(new ContentConsumer(201))
                .onFailure(new FailedRequestConsumer())
                .build();
    }

    private DefaultHandler<RoutingContext, UpdateDesignRequest, UpdateDesignResponse, Content> createUpdateDesignHandler(Store store) {
        return DefaultHandler.<RoutingContext, UpdateDesignRequest, UpdateDesignResponse, Content>builder()
                .withInputMapper(new UpdateDesignRequestMapper())
                .withOutputMapper(new UpdateDesignResponseMapper())
                .withController(new UpdateDesignController(store))
                .onSuccess(new ContentConsumer(200))
                .onFailure(new FailedRequestConsumer())
                .build();
    }

    private DefaultHandler<RoutingContext, ListDesignsRequest, ListDesignsResponse, Content> createListDesignsHandler(Store store) {
        return DefaultHandler.<RoutingContext, ListDesignsRequest, ListDesignsResponse, Content>builder()
                .withInputMapper(new ListDesignsRequestMapper())
                .withOutputMapper(new ListDesignsResponseMapper())
                .withController(new ListDesignsController(store))
                .onSuccess(new ContentConsumer(200))
                .onFailure(new FailedRequestConsumer())
                .build();
    }

    private DefaultHandler<RoutingContext, LoadDesignRequest, LoadDesignResponse, Content> createLoadDesignHandler(Store store) {
        return DefaultHandler.<RoutingContext, LoadDesignRequest, LoadDesignResponse, Content>builder()
                .withInputMapper(new LoadDesignRequestMapper())
                .withOutputMapper(new LoadDesignResponseMapper())
                .withController(new LoadDesignController(store))
                .onSuccess(new ContentConsumer(200, 404))
                .onFailure(new FailedRequestConsumer())
                .build();
    }

    private DefaultHandler<RoutingContext, GetStatusRequest, GetStatusResponse, Content> createGetStatusHandler(Store store) {
        return DefaultHandler.<RoutingContext, GetStatusRequest, GetStatusResponse, Content>builder()
                .withInputMapper(new GetStatusRequestMapper())
                .withOutputMapper(new GetStatusResponseMapper())
                .withController(new GetStatusController(store))
                .onSuccess(new ContentConsumer(200))
                .onFailure(new FailedRequestConsumer())
                .build();
    }

    private DefaultHandler<RoutingContext, ListStatusRequest, ListStatusResponse, Content> createListStatusHandler(Store store) {
        return DefaultHandler.<RoutingContext, ListStatusRequest, ListStatusResponse, Content>builder()
                .withInputMapper(new ListStatusRequestMapper())
                .withOutputMapper(new ListStatusResponseMapper())
                .withController(new ListStatusController(store))
                .onSuccess(new ContentConsumer(200))
                .onFailure(new FailedRequestConsumer())
                .build();
    }
}
