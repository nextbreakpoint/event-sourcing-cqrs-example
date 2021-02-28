package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.Environment;
import com.nextbreakpoint.blueprint.designs.handlers.EventsHandler;
import com.nextbreakpoint.blueprint.common.vertx.Failure;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import com.nextbreakpoint.blueprint.common.vertx.AccessHandler;
import com.nextbreakpoint.blueprint.common.vertx.CorsHandlerFactory;
import com.nextbreakpoint.blueprint.common.vertx.JWTProviderFactory;
import com.nextbreakpoint.blueprint.common.vertx.KafkaClientFactory;
import com.nextbreakpoint.blueprint.common.vertx.ResponseHelper;
import com.nextbreakpoint.blueprint.common.vertx.ServerUtil;
import com.nextbreakpoint.blueprint.designs.controllers.DesignChangedController;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Launcher;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.http.HttpServer;
import io.vertx.rxjava.ext.auth.jwt.JWTAuth;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.handler.BodyHandler;
import io.vertx.rxjava.ext.web.handler.CookieHandler;
import io.vertx.rxjava.ext.web.handler.CorsHandler;
import io.vertx.rxjava.ext.web.handler.LoggerHandler;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumer;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumerRecord;
import rx.Single;

import java.util.HashMap;
import java.util.Map;

import static com.nextbreakpoint.blueprint.common.core.Authority.ADMIN;
import static com.nextbreakpoint.blueprint.common.core.Authority.ANONYMOUS;
import static com.nextbreakpoint.blueprint.common.core.Authority.GUEST;
import static com.nextbreakpoint.blueprint.common.core.Headers.ACCEPT;
import static com.nextbreakpoint.blueprint.common.core.Headers.AUTHORIZATION;
import static com.nextbreakpoint.blueprint.common.core.Headers.CONTENT_TYPE;
import static com.nextbreakpoint.blueprint.common.core.Headers.COOKIE;
import static com.nextbreakpoint.blueprint.common.core.Headers.X_MODIFIED;
import static com.nextbreakpoint.blueprint.common.core.Headers.X_TRACE_ID;
import static com.nextbreakpoint.blueprint.common.core.Headers.X_XSRF_TOKEN;
import static com.nextbreakpoint.blueprint.common.vertx.ServerUtil.UUID_REGEXP;
import static java.util.Arrays.asList;

public class Verticle extends AbstractVerticle {
    private final Logger logger = LoggerFactory.getLogger(Verticle.class.getName());

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

        vertx.<Void>rxExecuteBlocking(future -> initServer(config, future))
                .subscribe(x -> startFuture.complete(), err -> startFuture.fail(err));
    }

    @Override
    public void stop(Future<Void> stopFuture) {
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
        final Environment environment = Environment.getDefaultEnvironment();

        final Integer port = Integer.parseInt(environment.resolve(config.getString("host_port")));

        final String originPattern = environment.resolve(config.getString("origin_pattern"));

        final String sseTopic = environment.resolve(config.getString("sse_topic"));

        final JWTAuth jwtProvider = JWTProviderFactory.create(environment, vertx, config);

        final KafkaConsumer<String, String> consumer = KafkaClientFactory.createConsumer(environment, vertx, config);

        final Router mainRouter = Router.router(vertx);

        mainRouter.route().handler(LoggerHandler.create());
        mainRouter.route().handler(BodyHandler.create());
        mainRouter.route().handler(CookieHandler.create());

        final CorsHandler corsHandler = CorsHandlerFactory.createWithAll(originPattern, asList(COOKIE, AUTHORIZATION, CONTENT_TYPE, ACCEPT, X_XSRF_TOKEN, X_MODIFIED, X_TRACE_ID));

        mainRouter.route("/*").handler(corsHandler);

        final Handler<RoutingContext> onAccessDenied = routingContext -> routingContext.fail(Failure.accessDenied("Authorisation failed"));

        mainRouter.route().failureHandler(ResponseHelper::sendFailure);

        final Handler<RoutingContext> eventHandler = new AccessHandler(jwtProvider, EventsHandler.create(vertx), onAccessDenied, asList(ANONYMOUS, ADMIN, GUEST));

        mainRouter.getWithRegex("/sse/designs/([0-9]+)/" + UUID_REGEXP)
                .handler(eventHandler);

        mainRouter.getWithRegex("/sse/designs/([0-9]+)")
                .handler(eventHandler);

        mainRouter.options("/*")
                .handler(ResponseHelper::sendNoContent);

        final Map<String, Handler<Message>> handlers = new HashMap<>();

        handlers.put(MessageType.DESIGN_CHANGED, Factory.createDesignChangedHandler(new DesignChangedController(vertx, "events.handler.input")));

        consumer.handler(record -> processRecord(handlers, record))
                .rxSubscribe(sseTopic)
                .doOnError(this::handleError)
                .subscribe();

        final HttpServerOptions options = ServerUtil.makeServerOptions(environment, config);

        server = vertx.createHttpServer(options)
                .requestHandler(mainRouter)
                .listen(port);

        return null;
    }

    private void handleError(Throwable err) {
        logger.error("Cannot process message", err);
    }

    private void processRecord(Map<String, Handler<Message>> handlers, KafkaConsumerRecord<String, String> record) {
        final Message message = Json.decodeValue(record.value(), Message.class);
        final Handler<Message> handler = handlers.get(message.getMessageType());
        logger.info("Received message " + message.getMessageType() + " (" + message.getMessageId() + ")");
        if (handler != null) {
            logger.info("Processing message " + message.getMessageType() + " (" + message.getMessageId() + ")");
            handler.handle(message);
        }
    }
}
