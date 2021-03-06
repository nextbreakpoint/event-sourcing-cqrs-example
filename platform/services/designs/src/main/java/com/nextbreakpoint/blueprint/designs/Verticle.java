package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.Environment;
import com.nextbreakpoint.blueprint.designs.persistence.MySQLStore;
import com.nextbreakpoint.blueprint.common.vertx.Failure;
import com.nextbreakpoint.blueprint.common.vertx.AccessHandler;
import com.nextbreakpoint.blueprint.common.vertx.CorsHandlerFactory;
import com.nextbreakpoint.blueprint.common.vertx.JDBCClientFactory;
import com.nextbreakpoint.blueprint.common.vertx.JWTProviderFactory;
import com.nextbreakpoint.blueprint.common.vertx.KafkaClientFactory;
import com.nextbreakpoint.blueprint.common.vertx.MDCHandler;
import com.nextbreakpoint.blueprint.common.vertx.ResponseHelper;
import com.nextbreakpoint.blueprint.common.vertx.ServerUtil;
import com.nextbreakpoint.blueprint.designs.handlers.TileHandler;
import io.vertx.core.Handler;
import io.vertx.core.Launcher;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.Promise;
import io.vertx.rxjava.core.WorkerExecutor;
import io.vertx.rxjava.core.http.HttpServer;
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
import rx.Single;

import static com.nextbreakpoint.blueprint.common.core.Authority.ADMIN;
import static com.nextbreakpoint.blueprint.common.core.Authority.ANONYMOUS;
import static com.nextbreakpoint.blueprint.common.core.Authority.GUEST;
import static com.nextbreakpoint.blueprint.common.core.ContentType.APPLICATION_JSON;
import static com.nextbreakpoint.blueprint.common.core.ContentType.IMAGE_PNG;
import static com.nextbreakpoint.blueprint.common.core.Headers.ACCEPT;
import static com.nextbreakpoint.blueprint.common.core.Headers.AUTHORIZATION;
import static com.nextbreakpoint.blueprint.common.core.Headers.CONTENT_TYPE;
import static com.nextbreakpoint.blueprint.common.core.Headers.X_MODIFIED;
import static com.nextbreakpoint.blueprint.common.core.Headers.X_TRACE_ID;
import static com.nextbreakpoint.blueprint.common.core.Headers.X_XSRF_TOKEN;
import static com.nextbreakpoint.blueprint.common.vertx.ServerUtil.UUID_REGEXP;
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
        Single.fromCallable(this::createServer).subscribe(httpServer -> promise.complete(), promise::fail);
    }

    private HttpServer createServer() {
        final JsonObject config = vertx.getOrCreateContext().config();

        final Environment environment = Environment.getDefaultEnvironment();

        final WorkerExecutor executor = createWorkerExecutor(environment, config);

        final Integer port = Integer.parseInt(environment.resolve(config.getString("host_port")));

        final String originPattern = environment.resolve(config.getString("origin_pattern"));

        final String sseTopic = environment.resolve(config.getString("sse_topic"));

        final String messageSource = environment.resolve(config.getString("message_source"));

        final JWTAuth jwtProvider = JWTProviderFactory.create(environment, vertx, config);

        final KafkaProducer<String, String> producer = KafkaClientFactory.createProducer(environment, vertx, config);

        final JDBCClient jdbcClient = JDBCClientFactory.create(environment, vertx, config);

        final Store store = new MySQLStore(jdbcClient);

        final Router mainRouter = Router.router(vertx);

        mainRouter.route().handler(MDCHandler.create());
        mainRouter.route().handler(LoggerHandler.create(true, LoggerFormat.DEFAULT));
        mainRouter.route().handler(BodyHandler.create());
//        mainRouter.route().handler(CookieHandler.create());
        mainRouter.route().handler(TimeoutHandler.create(90000));

        final CorsHandler corsHandler = CorsHandlerFactory.createWithAll(originPattern, asList(AUTHORIZATION, CONTENT_TYPE, ACCEPT, X_XSRF_TOKEN, X_MODIFIED, X_TRACE_ID), asList(CONTENT_TYPE, X_XSRF_TOKEN, X_MODIFIED, X_TRACE_ID));

        mainRouter.route("/*").handler(corsHandler);

        final Handler<RoutingContext> onAccessDenied = routingContext -> routingContext.fail(Failure.accessDenied("Authorisation failed"));

        final Handler<RoutingContext>  getTileHandler = new AccessHandler(jwtProvider, new TileHandler(store, executor), onAccessDenied, asList(ADMIN, GUEST, ANONYMOUS));

        final Handler<RoutingContext>  listDesignsHandler = new AccessHandler(jwtProvider, createListDesignsHandler(store), onAccessDenied, asList(ADMIN, GUEST, ANONYMOUS));

        final Handler<RoutingContext>  loadDesignHandler = new AccessHandler(jwtProvider, createLoadDesignHandler(store), onAccessDenied, asList(ADMIN, GUEST, ANONYMOUS));

        final Handler<RoutingContext>  insertDesignHandler = new AccessHandler(jwtProvider, createInsertDesignHandler(store, sseTopic, messageSource, producer), onAccessDenied, asList(ADMIN));

        final Handler<RoutingContext>  updateDesignHandler = new AccessHandler(jwtProvider, createUpdateDesignHandler(store, sseTopic, messageSource, producer), onAccessDenied, asList(ADMIN));

        final Handler<RoutingContext>  deleteDesignHandler = new AccessHandler(jwtProvider, createDeleteDesignHandler(store, sseTopic, messageSource, producer), onAccessDenied, asList(ADMIN));

        mainRouter.get("/designs/:uuid/:zoom/:x/:y/:size.png")
                .produces(IMAGE_PNG)
                .handler(getTileHandler);

        mainRouter.get("/designs")
                .produces(APPLICATION_JSON)
                .handler(listDesignsHandler);

        mainRouter.getWithRegex("/designs/" + UUID_REGEXP)
                .produces(APPLICATION_JSON)
                .handler(loadDesignHandler);

        mainRouter.post("/designs")
                .produces(APPLICATION_JSON)
                .consumes(APPLICATION_JSON)
                .handler(insertDesignHandler);

        mainRouter.putWithRegex("/designs/" + UUID_REGEXP)
                .produces(APPLICATION_JSON)
                .consumes(APPLICATION_JSON)
                .handler(updateDesignHandler);

        mainRouter.deleteWithRegex("/designs/" + UUID_REGEXP)
                .produces(APPLICATION_JSON)
                .handler(deleteDesignHandler);

        mainRouter.options("/*")
                .handler(ResponseHelper::sendNoContent);

        mainRouter.route().failureHandler(ResponseHelper::sendFailure);

        final HttpServerOptions options = ServerUtil.makeServerOptions(environment, config);

        return vertx.createHttpServer(options)
                .requestHandler(mainRouter::handle)
                .rxListen(port)
                .doOnSuccess(result -> logger.info("Service listening on port " + port))
                .toBlocking()
                .value();
    }

    private WorkerExecutor createWorkerExecutor(Environment environment, JsonObject config) {
        final int poolSize = Runtime.getRuntime().availableProcessors();
        final long maxExecuteTime = Integer.parseInt(environment.resolve(config.getString("max_execution_time_in_millis", "2000"))) * 1000000L;
        return vertx.createSharedWorkerExecutor("worker", poolSize, maxExecuteTime);
    }
}
