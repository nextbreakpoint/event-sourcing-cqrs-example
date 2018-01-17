package com.nextbreakpoint.shop.designs;

import com.nextbreakpoint.shop.common.AccessHandler;
import com.nextbreakpoint.shop.common.DelegateHandler;
import com.nextbreakpoint.shop.common.Failure;
import com.nextbreakpoint.shop.common.GraphiteManager;
import com.nextbreakpoint.shop.common.JDBCClientFactory;
import com.nextbreakpoint.shop.common.JWTProviderFactory;
import com.nextbreakpoint.shop.common.LiquibaseManager;
import com.nextbreakpoint.shop.common.ResponseHelper;
import com.nextbreakpoint.shop.common.CORSHandlerFactory;
import com.nextbreakpoint.shop.common.ServerUtil;
import com.nextbreakpoint.shop.common.FailedRequestHandler;
import com.nextbreakpoint.shop.common.ContentHandler;
import com.nextbreakpoint.shop.designs.delete.DeleteDesignRequest;
import com.nextbreakpoint.shop.designs.delete.DeleteDesignController;
import com.nextbreakpoint.shop.designs.delete.DeleteDesignRequestMapper;
import com.nextbreakpoint.shop.designs.delete.DeleteDesignResponse;
import com.nextbreakpoint.shop.designs.delete.DeleteDesignResponseMapper;
import com.nextbreakpoint.shop.common.NoContentHandler;
import com.nextbreakpoint.shop.designs.delete.DeleteDesignsRequest;
import com.nextbreakpoint.shop.designs.delete.DeleteDesignsController;
import com.nextbreakpoint.shop.designs.delete.DeleteDesignsRequestMapper;
import com.nextbreakpoint.shop.designs.delete.DeleteDesignsResponse;
import com.nextbreakpoint.shop.designs.delete.DeleteDesignsResponseMapper;
import com.nextbreakpoint.shop.designs.get.GetTileHandler;
import com.nextbreakpoint.shop.designs.insert.InsertDesignRequest;
import com.nextbreakpoint.shop.designs.insert.InsertDesignController;
import com.nextbreakpoint.shop.designs.insert.InsertDesignRequestMapper;
import com.nextbreakpoint.shop.designs.insert.InsertDesignResponse;
import com.nextbreakpoint.shop.designs.insert.InsertDesignResponseMapper;
import com.nextbreakpoint.shop.designs.list.ListDesignsRequest;
import com.nextbreakpoint.shop.designs.list.ListDesignsController;
import com.nextbreakpoint.shop.designs.list.ListDesignsRequestMapper;
import com.nextbreakpoint.shop.designs.list.ListDesignsResponse;
import com.nextbreakpoint.shop.designs.list.ListDesignsResponseMapper;
import com.nextbreakpoint.shop.designs.load.LoadDesignRequest;
import com.nextbreakpoint.shop.designs.load.LoadDesignController;
import com.nextbreakpoint.shop.designs.load.LoadDesignRequestMapper;
import com.nextbreakpoint.shop.designs.load.LoadDesignResponse;
import com.nextbreakpoint.shop.designs.load.LoadDesignResponseMapper;
import com.nextbreakpoint.shop.designs.get.GetStatusRequest;
import com.nextbreakpoint.shop.designs.get.GetStatusController;
import com.nextbreakpoint.shop.designs.get.GetStatusRequestMapper;
import com.nextbreakpoint.shop.designs.get.GetStatusResponse;
import com.nextbreakpoint.shop.designs.get.GetStatusResponseMapper;
import com.nextbreakpoint.shop.designs.list.ListStatusRequest;
import com.nextbreakpoint.shop.designs.list.ListStatusController;
import com.nextbreakpoint.shop.designs.list.ListStatusRequestMapper;
import com.nextbreakpoint.shop.designs.list.ListStatusResponse;
import com.nextbreakpoint.shop.designs.list.ListStatusResponseMapper;
import com.nextbreakpoint.shop.designs.update.UpdateDesignRequest;
import com.nextbreakpoint.shop.designs.update.UpdateDesignController;
import com.nextbreakpoint.shop.designs.update.UpdateDesignRequestMapper;
import com.nextbreakpoint.shop.designs.update.UpdateDesignResponse;
import com.nextbreakpoint.shop.designs.update.UpdateDesignResponseMapper;
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

        final Router mainRouter = Router.router(vertx);

        final Router apiRouter = Router.router(vertx);

        mainRouter.route().handler(LoggerHandler.create());
        mainRouter.route().handler(BodyHandler.create());
        mainRouter.route().handler(CookieHandler.create());
        mainRouter.route().handler(TimeoutHandler.create(30000));

        final CorsHandler corsHandler = CORSHandlerFactory.createWithAll(webUrl, asList(AUTHORIZATION, CONTENT_TYPE, ACCEPT, XSRFTOKEN, MODIFIED), asList(CONTENT_TYPE, XSRFTOKEN, MODIFIED));

        apiRouter.route("/designs/*").handler(corsHandler);

        final Consumer<RoutingContext> onAccessDenied = rc -> rc.fail(Failure.accessDenied());

        apiRouter.get("/designs/*").handler(new AccessHandler(jwtProvider, onAccessDenied, asList(ADMIN, GUEST, ANONYMOUS)));
        apiRouter.put("/designs/*").handler(new AccessHandler(jwtProvider, onAccessDenied, asList(ADMIN)));
        apiRouter.post("/designs").handler(new AccessHandler(jwtProvider, onAccessDenied, asList(ADMIN)));
        apiRouter.patch("/designs/*").handler(new AccessHandler(jwtProvider, onAccessDenied, asList(ADMIN)));
        apiRouter.delete("/designs/*").handler(new AccessHandler(jwtProvider, onAccessDenied, asList(ADMIN)));

        apiRouter.get("/designs/:uuid/:zoom/:x/:y/:size.png").produces(IMAGE_PNG).handler(new GetTileHandler(store, executor));

        apiRouter.get("/designs").produces(APPLICATION_JSON).handler(createListDesignsHandler(store));

        apiRouter.getWithRegex("/designs/" + UUID_REGEXP).produces(APPLICATION_JSON).handler(createLoadDesignHandler(store));

        apiRouter.post("/designs").produces(APPLICATION_JSON).consumes(APPLICATION_JSON).handler(createInsertDesignHandler(store));
        apiRouter.putWithRegex("/designs/" + UUID_REGEXP).produces(APPLICATION_JSON).consumes(APPLICATION_JSON).handler(createUpdateDesignHandler(store));

        apiRouter.deleteWithRegex("/designs/" + UUID_REGEXP).produces(APPLICATION_JSON).handler(createDeleteDesignHandler(store));

        apiRouter.delete("/designs").handler(createDeleteDesignsHandler(store));

        apiRouter.options("/designs/*").handler(routingContext -> routingContext.response().setStatusCode(204).end());
        apiRouter.options("/designs").handler(routingContext -> routingContext.response().setStatusCode(204).end());

        apiRouter.get("/designs/state").produces(APPLICATION_JSON).consumes(APPLICATION_JSON).handler(createGetStatusHandler(store));
        apiRouter.get("/designs/state").produces(APPLICATION_JSON).handler(createListStatusHandler(store));

        mainRouter.route().failureHandler(routingContext -> ResponseHelper.sendFailure(routingContext));

        mainRouter.mountSubRouter("/api", apiRouter);

        server = vertx.createHttpServer(ServerUtil.makeServerOptions(config)).requestHandler(mainRouter::accept).listen(port);

        return null;
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

    private DelegateHandler<ListDesignsRequest, ListDesignsResponse> createListDesignsHandler(Store store) {
        return DelegateHandler.<ListDesignsRequest, ListDesignsResponse>builder()
                .with(new ListDesignsRequestMapper())
                .with(new ListDesignsResponseMapper())
                .with(new ListDesignsController(store))
                .with(new ContentHandler(200))
                .with(new FailedRequestHandler())
                .build();
    }

    private DelegateHandler<LoadDesignRequest, LoadDesignResponse> createLoadDesignHandler(Store store) {
        return DelegateHandler.<LoadDesignRequest, LoadDesignResponse>builder()
                .with(new LoadDesignRequestMapper())
                .with(new LoadDesignResponseMapper())
                .with(new LoadDesignController(store))
                .with(new ContentHandler(200, 404))
                .with(new FailedRequestHandler())
                .build();
    }

    private DelegateHandler<GetStatusRequest, GetStatusResponse> createGetStatusHandler(Store store) {
        return DelegateHandler.<GetStatusRequest, GetStatusResponse>builder()
                .with(new GetStatusRequestMapper())
                .with(new GetStatusResponseMapper())
                .with(new GetStatusController(store))
                .with(new ContentHandler(200))
                .with(new FailedRequestHandler())
                .build();
    }

    private DelegateHandler<ListStatusRequest, ListStatusResponse> createListStatusHandler(Store store) {
        return DelegateHandler.<ListStatusRequest, ListStatusResponse>builder()
                .with(new ListStatusRequestMapper())
                .with(new ListStatusResponseMapper())
                .with(new ListStatusController(store))
                .with(new ContentHandler(200))
                .with(new FailedRequestHandler())
                .build();
    }

    private WorkerExecutor createWorkerExecutor(JsonObject config) {
        final int poolSize = Runtime.getRuntime().availableProcessors();
        final long maxExecuteTime = config.getInteger("max_execution_time_in_millis", 2000) * 1000000L;
        return vertx.createSharedWorkerExecutor("worker", poolSize, maxExecuteTime);
    }
}
