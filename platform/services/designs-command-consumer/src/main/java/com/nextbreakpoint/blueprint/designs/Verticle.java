package com.nextbreakpoint.blueprint.designs;

import com.datastax.driver.core.Session;
import com.nextbreakpoint.blueprint.common.core.Environment;
import com.nextbreakpoint.blueprint.designs.common.CommandFailureConsumer;
import com.nextbreakpoint.blueprint.designs.common.CommandSuccessConsumer;
import com.nextbreakpoint.blueprint.designs.common.ViewFailureConsumer;
import com.nextbreakpoint.blueprint.designs.common.ViewSuccessConsumer;
import com.nextbreakpoint.blueprint.designs.model.RecordAndMessage;
import com.nextbreakpoint.blueprint.common.cassandra.CassandraClusterFactory;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import com.nextbreakpoint.blueprint.common.vertx.CorsHandlerFactory;
import com.nextbreakpoint.blueprint.common.vertx.JWTProviderFactory;
import com.nextbreakpoint.blueprint.common.vertx.KafkaClientFactory;
import com.nextbreakpoint.blueprint.common.vertx.MDCHandler;
import com.nextbreakpoint.blueprint.common.vertx.ResponseHelper;
import com.nextbreakpoint.blueprint.common.vertx.ServerUtil;
import com.nextbreakpoint.blueprint.designs.persistence.CassandraStore;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Launcher;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.http.HttpServer;
import io.vertx.rxjava.ext.auth.jwt.JWTAuth;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.handler.BodyHandler;
import io.vertx.rxjava.ext.web.handler.CookieHandler;
import io.vertx.rxjava.ext.web.handler.CorsHandler;
import io.vertx.rxjava.ext.web.handler.LoggerHandler;
import io.vertx.rxjava.ext.web.handler.TimeoutHandler;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumer;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import rx.Single;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static com.nextbreakpoint.blueprint.common.core.Headers.ACCEPT;
import static com.nextbreakpoint.blueprint.common.core.Headers.AUTHORIZATION;
import static com.nextbreakpoint.blueprint.common.core.Headers.CONTENT_TYPE;
import static com.nextbreakpoint.blueprint.common.core.Headers.X_MODIFIED;
import static com.nextbreakpoint.blueprint.common.core.Headers.X_XSRF_TOKEN;
import static com.nextbreakpoint.blueprint.designs.Factory.createDeleteDesignHandler;
import static com.nextbreakpoint.blueprint.designs.Factory.createDesignChangedHandler;
import static com.nextbreakpoint.blueprint.designs.Factory.createInsertDesignHandler;
import static com.nextbreakpoint.blueprint.designs.Factory.createUpdateDesignHandler;
import static java.util.Arrays.asList;

public class Verticle extends AbstractVerticle {
    private final Logger logger = LoggerFactory.getLogger(Verticle.class.getName());

    private HttpServer server;

    public static void main(String[] args) {
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

        final String keyspace = environment.resolve(config.getString("cassandra_keyspace"));

        final String eventsTopic = environment.resolve(config.getString("events_topic"));

        final String sseTopic = environment.resolve(config.getString("sse_topic"));

        final String viewTopic = environment.resolve(config.getString("view_topic"));

        final String messageSource = environment.resolve(config.getString("message_source"));

        final JWTAuth jwtProvider = JWTProviderFactory.create(environment, vertx, config);

        final KafkaProducer<String, String> producer = KafkaClientFactory.createProducer(environment, vertx, config);

        final KafkaConsumer<String, String> commandConsumer = KafkaClientFactory.createConsumer(environment, vertx, config);

        final KafkaConsumer<String, String> viewConsumer = KafkaClientFactory.createConsumer(environment, vertx, config);

        final Supplier<Session> supplier = () -> CassandraClusterFactory.create(environment, config).connect(keyspace);

        final Store store = new CassandraStore(supplier);

        final Router mainRouter = Router.router(vertx);

        mainRouter.route().handler(MDCHandler.create());
        mainRouter.route().handler(LoggerHandler.create(true, LoggerFormat.DEFAULT));
        mainRouter.route().handler(BodyHandler.create());
        mainRouter.route().handler(CookieHandler.create());
        mainRouter.route().handler(TimeoutHandler.create(90000));

        final CorsHandler corsHandler = CorsHandlerFactory.createWithAll(originPattern, asList(AUTHORIZATION, CONTENT_TYPE, ACCEPT, X_XSRF_TOKEN, X_MODIFIED), asList(CONTENT_TYPE, X_XSRF_TOKEN, X_MODIFIED));

        mainRouter.route("/*").handler(corsHandler);

        mainRouter.options("/*").handler(ResponseHelper::sendNoContent);

        mainRouter.route().failureHandler(ResponseHelper::sendFailure);

        final Map<String, Handler<RecordAndMessage>> commandHandlers = new HashMap<>();

        commandHandlers.put(MessageType.DESIGN_INSERT, createInsertDesignHandler(store, viewTopic, producer, messageSource, new CommandSuccessConsumer(commandConsumer), new CommandFailureConsumer(commandConsumer)));
        commandHandlers.put(MessageType.DESIGN_UPDATE, createUpdateDesignHandler(store, viewTopic, producer, messageSource, new CommandSuccessConsumer(commandConsumer), new CommandFailureConsumer(commandConsumer)));
        commandHandlers.put(MessageType.DESIGN_DELETE, createDeleteDesignHandler(store, viewTopic, producer, messageSource, new CommandSuccessConsumer(commandConsumer), new CommandFailureConsumer(commandConsumer)));

        final Map<String, Handler<RecordAndMessage>> viewHandlers = new HashMap<>();

        viewHandlers.put(MessageType.DESIGN_CHANGED, createDesignChangedHandler(store, sseTopic, producer, messageSource, new ViewSuccessConsumer(viewConsumer), new ViewFailureConsumer(viewConsumer)));

        commandConsumer.handler(record -> processRecord(commandHandlers, record))
                .rxSubscribe(eventsTopic)
                .doOnError(this::handleError)
                .subscribe();

        viewConsumer.handler(record -> processRecord(viewHandlers, record))
                .rxSubscribe(viewTopic)
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

    private void processRecord(Map<String, Handler<RecordAndMessage>> handlers, KafkaConsumerRecord<String, String> record) {
        final Message message = Json.decodeValue(record.value(), Message.class);
        final Handler<RecordAndMessage> handler = handlers.get(message.getMessageType());
        if (handler != null) {
            handler.handle(new RecordAndMessage(record, message));
        } else {
            logger.warn("Can't handle message {} with type {}", record.key(), message.getMessageType());
        }
    }
}
