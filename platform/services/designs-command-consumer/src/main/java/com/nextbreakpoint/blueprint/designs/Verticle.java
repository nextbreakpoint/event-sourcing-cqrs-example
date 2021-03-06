package com.nextbreakpoint.blueprint.designs;

import com.datastax.driver.core.Session;
import com.nextbreakpoint.blueprint.common.core.Environment;
import com.nextbreakpoint.blueprint.designs.common.CommandFailureConsumer;
import com.nextbreakpoint.blueprint.designs.common.CommandSuccessConsumer;
import com.nextbreakpoint.blueprint.designs.common.ViewFailureConsumer;
import com.nextbreakpoint.blueprint.designs.common.ViewSuccessConsumer;
import com.nextbreakpoint.blueprint.designs.model.RecordAndMessage;
import com.nextbreakpoint.blueprint.common.vertx.CassandraClusterFactory;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import com.nextbreakpoint.blueprint.common.vertx.CorsHandlerFactory;
import com.nextbreakpoint.blueprint.common.vertx.JWTProviderFactory;
import com.nextbreakpoint.blueprint.common.vertx.KafkaClientFactory;
import com.nextbreakpoint.blueprint.common.vertx.MDCHandler;
import com.nextbreakpoint.blueprint.common.vertx.ResponseHelper;
import com.nextbreakpoint.blueprint.common.vertx.ServerUtil;
import com.nextbreakpoint.blueprint.designs.persistence.CassandraStore;
import io.vertx.core.Handler;
import io.vertx.core.Launcher;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.Promise;
import io.vertx.rxjava.core.http.HttpServer;
import io.vertx.rxjava.ext.auth.jwt.JWTAuth;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.handler.BodyHandler;
import io.vertx.rxjava.ext.web.handler.CorsHandler;
import io.vertx.rxjava.ext.web.handler.LoggerHandler;
import io.vertx.rxjava.ext.web.handler.TimeoutHandler;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumer;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import rx.Completable;
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
//        mainRouter.route().handler(CookieHandler.create());
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

        return vertx.createHttpServer(options)
                .requestHandler(mainRouter::handle)
                .rxListen(port)
                .doOnSuccess(result -> logger.info("Service listening on port " + port))
                .toBlocking()
                .value();
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
            logger.warn("Can't handle message with type: " + message.getMessageType());
        }
    }
}
