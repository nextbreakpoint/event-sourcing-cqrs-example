package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.Environment;
import com.nextbreakpoint.blueprint.common.vertx.*;
import com.nextbreakpoint.blueprint.designs.persistence.MySQLStore;
import com.nextbreakpoint.blueprint.designs.handlers.TileHandler;
import io.vertx.core.Handler;
import io.vertx.core.Launcher;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.Promise;
import io.vertx.rxjava.core.WorkerExecutor;
import io.vertx.rxjava.ext.auth.jwt.JWTAuth;
import io.vertx.rxjava.ext.jdbc.JDBCClient;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.handler.BodyHandler;
import io.vertx.rxjava.ext.web.handler.CorsHandler;
import io.vertx.rxjava.ext.web.handler.LoggerHandler;
import io.vertx.rxjava.ext.web.handler.TimeoutHandler;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import rx.Completable;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static com.nextbreakpoint.blueprint.common.core.Authority.ADMIN;
import static com.nextbreakpoint.blueprint.common.core.Authority.ANONYMOUS;
import static com.nextbreakpoint.blueprint.common.core.Authority.GUEST;
import static com.nextbreakpoint.blueprint.common.core.Headers.ACCEPT;
import static com.nextbreakpoint.blueprint.common.core.Headers.AUTHORIZATION;
import static com.nextbreakpoint.blueprint.common.core.Headers.CONTENT_TYPE;
import static com.nextbreakpoint.blueprint.common.core.Headers.X_MODIFIED;
import static com.nextbreakpoint.blueprint.common.core.Headers.X_TRACE_ID;
import static com.nextbreakpoint.blueprint.common.core.Headers.X_XSRF_TOKEN;
import static com.nextbreakpoint.blueprint.designs.Factory.createDeleteDesignHandler;
import static com.nextbreakpoint.blueprint.designs.Factory.createInsertDesignHandler;
import static com.nextbreakpoint.blueprint.designs.Factory.createListDesignsHandler;
import static com.nextbreakpoint.blueprint.designs.Factory.createLoadDesignHandler;
import static com.nextbreakpoint.blueprint.designs.Factory.createUpdateDesignHandler;
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
        try {
            final JsonObject config = vertx.getOrCreateContext().config();

            final Environment environment = Environment.getDefaultEnvironment();

            final Executor executor = Executors.newSingleThreadExecutor();

            final WorkerExecutor workerExecutor = createWorkerExecutor(environment, config);

            final int port = Integer.parseInt(environment.resolve(config.getString("host_port")));

            final String originPattern = environment.resolve(config.getString("origin_pattern"));

            final String sseTopic = environment.resolve(config.getString("sse_topic"));

            final String messageSource = environment.resolve(config.getString("message_source"));

            final JWTAuth jwtProvider = JWTProviderFactory.create(environment, vertx, config);

            final KafkaProducer<String, String> producer = KafkaClientFactory.createProducer(environment, vertx, config);

            final JDBCClient jdbcClient = JDBCClientFactory.create(environment, vertx, config);

            final Store store = new MySQLStore(jdbcClient);

            final Router mainRouter = Router.router(vertx);

            final CorsHandler corsHandler = CorsHandlerFactory.createWithAll(originPattern, asList(AUTHORIZATION, CONTENT_TYPE, ACCEPT, X_XSRF_TOKEN, X_MODIFIED, X_TRACE_ID), asList(CONTENT_TYPE, X_XSRF_TOKEN, X_MODIFIED, X_TRACE_ID));

            final Handler<RoutingContext> onAccessDenied = routingContext -> routingContext.fail(Failure.accessDenied("Authorisation failed"));

            final Handler<RoutingContext>  getTileHandler = new AccessHandler(jwtProvider, new TileHandler(store, workerExecutor), onAccessDenied, asList(ADMIN, GUEST, ANONYMOUS));

            final Handler<RoutingContext>  listDesignsHandler = new AccessHandler(jwtProvider, createListDesignsHandler(store), onAccessDenied, asList(ADMIN, GUEST, ANONYMOUS));

            final Handler<RoutingContext>  loadDesignHandler = new AccessHandler(jwtProvider, createLoadDesignHandler(store), onAccessDenied, asList(ADMIN, GUEST, ANONYMOUS));

            final Handler<RoutingContext>  insertDesignHandler = new AccessHandler(jwtProvider, createInsertDesignHandler(store, sseTopic, messageSource, producer), onAccessDenied, asList(ADMIN));

            final Handler<RoutingContext>  updateDesignHandler = new AccessHandler(jwtProvider, createUpdateDesignHandler(store, sseTopic, messageSource, producer), onAccessDenied, asList(ADMIN));

            final Handler<RoutingContext>  deleteDesignHandler = new AccessHandler(jwtProvider, createDeleteDesignHandler(store, sseTopic, messageSource, producer), onAccessDenied, asList(ADMIN));

            final Handler<RoutingContext> openapiHandler = new OpenApiHandler(vertx.getDelegate(), executor, "openapi.yaml");

            final String url = RouterBuilder.class.getClassLoader().getResource("openapi.yaml").toURI().toString();

            RouterBuilder.create(vertx.getDelegate(), url)
                    .onSuccess(routerBuilder -> {
                        routerBuilder.operation("getTile")
                                .handler(context -> getTileHandler.handle(RoutingContext.newInstance(context)));

                        routerBuilder.operation("listDesigns")
                                .handler(context -> listDesignsHandler.handle(RoutingContext.newInstance(context)));

                        routerBuilder.operation("loadDesign")
                                .handler(context -> loadDesignHandler.handle(RoutingContext.newInstance(context)));

                        routerBuilder.operation("insertDesign")
                                .handler(context -> insertDesignHandler.handle(RoutingContext.newInstance(context)));

                        routerBuilder.operation("updateDesign")
                                .handler(context -> updateDesignHandler.handle(RoutingContext.newInstance(context)));

                        routerBuilder.operation("deleteDesign")
                                .handler(context -> deleteDesignHandler.handle(RoutingContext.newInstance(context)));

                        final Router apiRouter = Router.newInstance(routerBuilder.createRouter());

                        mainRouter.route().handler(MDCHandler.create());
                        mainRouter.route().handler(LoggerHandler.create(true, LoggerFormat.DEFAULT));
                        mainRouter.route().handler(BodyHandler.create());
                        //mainRouter.route().handler(CookieHandler.create());
                        mainRouter.route().handler(TimeoutHandler.create(30000));

                        mainRouter.route("/*").handler(corsHandler);

                        mainRouter.mountSubRouter("/v1", apiRouter);

                        mainRouter.get("/v1/apidocs").handler(openapiHandler::handle);

                        mainRouter.options("/*").handler(ResponseHelper::sendNoContent);

                        mainRouter.route().failureHandler(ResponseHelper::sendFailure);

                        final HttpServerOptions options = ServerUtil.makeServerOptions(environment, config);

                        vertx.createHttpServer(options)
                                .requestHandler(mainRouter::handle)
                                .rxListen(port)
                                .doOnSuccess(result -> logger.info("Service listening on port " + port))
                                .doOnError(err -> logger.error("Can't create server", err))
                                .subscribe(result -> promise.complete(), promise::fail);
                    })
                    .onFailure(err -> {
                        logger.error("Can't create router", err);
                        promise.fail(err);
                    });
        } catch (Exception e) {
            logger.error("Failed to start server", e);
            promise.fail(e);
        }
    }

    private WorkerExecutor createWorkerExecutor(Environment environment, JsonObject config) {
        final int poolSize = Runtime.getRuntime().availableProcessors();
        final long maxExecuteTime = Integer.parseInt(environment.resolve(config.getString("max_execution_time_in_millis", "2000"))) * 1000000L;
        return vertx.createSharedWorkerExecutor("worker", poolSize, maxExecuteTime);
    }
}