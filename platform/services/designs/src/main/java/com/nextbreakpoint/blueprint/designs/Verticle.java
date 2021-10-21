package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.Environment;
import com.nextbreakpoint.blueprint.common.core.IOUtils;
import com.nextbreakpoint.blueprint.common.vertx.*;
import com.nextbreakpoint.blueprint.designs.handlers.TileHandler;
import com.nextbreakpoint.blueprint.designs.persistence.MySQLStore;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.VertxOptions;
import io.vertx.core.dns.AddressResolverOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.Promise;
import io.vertx.rxjava.core.Vertx;
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
import io.vertx.tracing.opentracing.OpenTracingOptions;
import rx.Completable;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static com.nextbreakpoint.blueprint.common.core.Authority.*;
import static com.nextbreakpoint.blueprint.common.core.Headers.*;
import static com.nextbreakpoint.blueprint.designs.Factory.*;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

public class Verticle extends AbstractVerticle {
    private static final Logger logger = LoggerFactory.getLogger(Verticle.class.getName());

    public static void main(String[] args) {
        try {
            final JsonObject config = loadConfig(args.length > 0 ? args[0] : "config/localhost.json");

            final VertxPrometheusOptions prometheusOptions = new VertxPrometheusOptions().setEnabled(true);

            final MicrometerMetricsOptions metricsOptions = new MicrometerMetricsOptions()
                    .setPrometheusOptions(prometheusOptions).setEnabled(true);

            final OpenTracingOptions tracingOptions = new OpenTracingOptions();

            final AddressResolverOptions addressResolverOptions = new AddressResolverOptions()
                    .setCacheNegativeTimeToLive(0)
                    .setCacheMaxTimeToLive(30);

            final VertxOptions vertxOptions = new VertxOptions()
                    .setAddressResolverOptions(addressResolverOptions)
                    .setMetricsOptions(metricsOptions)
                    .setTracingOptions(tracingOptions);

            final Vertx vertx = Vertx.vertx(vertxOptions);

            vertx.deployVerticle(new Verticle(), new DeploymentOptions().setConfig(config));
        } catch (Exception e) {
            logger.error("Can't start service", e);
        }
    }

    private static JsonObject loadConfig(String configPath) throws IOException {
        try (FileInputStream stream = new FileInputStream(configPath)) {
            return new JsonObject(IOUtils.toString(stream));
        }
    }

    @Override
    public Completable rxStart() {
        return vertx.rxExecuteBlocking(this::initServer)
                .doOnError(err -> logger.error("Failed to start server", err))
                .toCompletable();
    }

    private void initServer(Promise<Void> promise) {
        try {
            final JsonObject config = vertx.getOrCreateContext().config();

            final Environment environment = Environment.getDefaultEnvironment();

            final Executor executor = Executors.newSingleThreadExecutor();

            final WorkerExecutor workerExecutor = createWorkerExecutor(environment, config);

            final int port = Integer.parseInt(environment.resolve(config.getString("host_port")));

            final String originPattern = environment.resolve(config.getString("origin_pattern"));

            final String eventTopic = environment.resolve(config.getString("design_event_topic"));

            final String messageSource = environment.resolve(config.getString("message_source"));

            final JWTAuth jwtProvider = JWTProviderFactory.create(environment, vertx, config);

            final KafkaProducer<String, String> producer = KafkaClientFactory.createProducer(environment, vertx, config);

            final JDBCClient jdbcClient = JDBCClientFactory.create(environment, vertx, config);

            final Store store = new MySQLStore(jdbcClient);

            final Router mainRouter = Router.router(vertx);

            final CorsHandler corsHandler = CorsHandlerFactory.createWithAll(originPattern, asList(AUTHORIZATION, CONTENT_TYPE, ACCEPT, X_XSRF_TOKEN), asList(CONTENT_TYPE, X_XSRF_TOKEN));

            final Handler<RoutingContext> onAccessDenied = routingContext -> routingContext.fail(Failure.accessDenied("Authorisation failed"));

            final Handler<RoutingContext>  getTileHandler = new AccessHandler(jwtProvider, new TileHandler(store, workerExecutor), onAccessDenied, asList(ADMIN, GUEST, ANONYMOUS));

            final Handler<RoutingContext>  listDesignsHandler = new AccessHandler(jwtProvider, createListDesignsHandler(store), onAccessDenied, asList(ADMIN, GUEST, ANONYMOUS));

            final Handler<RoutingContext>  loadDesignHandler = new AccessHandler(jwtProvider, createLoadDesignHandler(store), onAccessDenied, asList(ADMIN, GUEST, ANONYMOUS));

            final Handler<RoutingContext>  insertDesignHandler = new AccessHandler(jwtProvider, createInsertDesignHandler(store, eventTopic, messageSource, producer), onAccessDenied, singletonList(ADMIN));

            final Handler<RoutingContext>  updateDesignHandler = new AccessHandler(jwtProvider, createUpdateDesignHandler(store, eventTopic, messageSource, producer), onAccessDenied, singletonList(ADMIN));

            final Handler<RoutingContext>  deleteDesignHandler = new AccessHandler(jwtProvider, createDeleteDesignHandler(store, eventTopic, messageSource, producer), onAccessDenied, singletonList(ADMIN));

            final Handler<RoutingContext> openapiHandler = new OpenApiHandler(vertx.getDelegate(), executor, "openapi.yaml");

            final URL resource = RouterBuilder.class.getClassLoader().getResource("openapi.yaml");

            if (resource == null) {
                throw new Exception("Cannot find resource openapi.yaml");
            }

            final String url = resource.toURI().toString();

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

                        mainRouter.get("/v1/apidocs").handler(openapiHandler);

                        mainRouter.options("/*").handler(ResponseHelper::sendNoContent);

                        mainRouter.route().failureHandler(ResponseHelper::sendFailure);

                        final HttpServerOptions options = ServerUtil.makeServerOptions(environment, config);

                        vertx.createHttpServer(options)
                                .requestHandler(mainRouter)
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
