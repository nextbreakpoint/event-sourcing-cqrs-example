package com.nextbreakpoint.shop.designs;

import com.nextbreakpoint.shop.common.model.Message;
import com.nextbreakpoint.shop.common.model.MessageType;
import com.nextbreakpoint.shop.common.vertx.AccessHandler;
import com.nextbreakpoint.shop.common.vertx.CORSHandlerFactory;
import com.nextbreakpoint.shop.common.model.Failure;
import com.nextbreakpoint.shop.common.graphite.GraphiteManager;
import com.nextbreakpoint.shop.common.vertx.JWTProviderFactory;
import com.nextbreakpoint.shop.common.vertx.KafkaClientFactory;
import com.nextbreakpoint.shop.common.vertx.ResponseHelper;
import com.nextbreakpoint.shop.common.vertx.ServerUtil;
import com.nextbreakpoint.shop.designs.handlers.watch.WatchHandler;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Launcher;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
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
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumer;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumerRecord;
import rx.Single;

import java.util.HashMap;
import java.util.Map;

import static com.nextbreakpoint.shop.common.model.Authority.ADMIN;
import static com.nextbreakpoint.shop.common.model.Headers.ACCEPT;
import static com.nextbreakpoint.shop.common.model.Headers.AUTHORIZATION;
import static com.nextbreakpoint.shop.common.model.Headers.CONTENT_TYPE;
import static com.nextbreakpoint.shop.common.model.Headers.X_MODIFIED;
import static com.nextbreakpoint.shop.common.model.Headers.X_XSRF_TOKEN;
import static com.nextbreakpoint.shop.common.vertx.ServerUtil.UUID_REGEXP;
import static com.nextbreakpoint.shop.designs.Factory.createDesignChangedHandler;
import static java.util.Arrays.asList;

public class Verticle extends AbstractVerticle {
    private final Logger logger = LoggerFactory.getLogger(Verticle.class.getName());

    private WorkerExecutor executor;

    private HttpServer server;

    public static void main(String[] args) {
        System.setProperty("crypto.policy", "unlimited");
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

        final String sseTopic = config.getString("sse_topic");

        final JWTAuth jwtProvider = JWTProviderFactory.create(vertx, config);

        final KafkaConsumer<String, String> consumer = KafkaClientFactory.createConsumer(vertx, config);

        final Router mainRouter = Router.router(vertx);

        final Router apiRouter = Router.router(vertx);

        mainRouter.route().handler(LoggerHandler.create());
        mainRouter.route().handler(BodyHandler.create());
        mainRouter.route().handler(CookieHandler.create());
        mainRouter.route().handler(TimeoutHandler.create(30000));

        final CorsHandler corsHandler = CORSHandlerFactory.createWithAll(webUrl, asList(AUTHORIZATION, CONTENT_TYPE, ACCEPT, X_XSRF_TOKEN, X_MODIFIED), asList(CONTENT_TYPE, X_XSRF_TOKEN, X_MODIFIED));

        apiRouter.route("/designs/*").handler(corsHandler);

        final Handler<RoutingContext> onAccessDenied = rc -> rc.fail(Failure.accessDenied("Authorisation failed"));

        final Handler consumerHandler = new AccessHandler(jwtProvider, createConsumerHandler(consumer), onAccessDenied, asList(ADMIN));

        mainRouter.route().failureHandler(ResponseHelper::sendFailure);

        apiRouter.post("/consumer")
                .handler(consumerHandler);

        apiRouter.options("/consumer/*")
                .handler(ResponseHelper::sendNoContent);

        apiRouter.getWithRegex("/events/([0-9]+)/" + UUID_REGEXP)
                .handler(WatchHandler.create(vertx));

        apiRouter.getWithRegex("/events/([0-9]+)")
                .handler(WatchHandler.create(vertx));

        mainRouter.mountSubRouter("/api", apiRouter);

        final Map<String, Handler<Message>> handlers = new HashMap<>();

        handlers.put(MessageType.DESIGN_CHANGED, createDesignChangedHandler(vertx, "watch.input"));

        consumer.handler(record -> processRecord(handlers, record))
                .rxSubscribe(sseTopic)
                .doOnError(this::handleError)
                .subscribe();

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

    private void handleError(Throwable err) {
        logger.error("Cannot process message", err);
    }

    private void processRecord(Map<String, Handler<Message>> handlers, KafkaConsumerRecord<String, String> record) {
        final Message message = Json.decodeValue(record.value(), Message.class);
        final Handler<Message> handler = handlers.get(message.getMessageType());
        if (handler != null) {
            handler.handle(message);
        }
    }

    private Handler<RoutingContext> createConsumerHandler(KafkaConsumer<String, String> consumer) {
        return routingContent -> {
            try {
                final JsonObject body = routingContent.getBodyAsJson();
                final String command = body.getString("command");
                switch (command) {
                    case "pause": consumer.pause(); break;
                    case "resume": consumer.resume(); break;
                    default: break;
                }
                routingContent.response().setStatusCode(206).end();
            } catch (Exception e) {
                routingContent.fail(e);
            }
        };
    }
}
