package com.nextbreakpoint.shop.accounts;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.nextbreakpoint.shop.accounts.controllers.delete.DeleteAccountController;
import com.nextbreakpoint.shop.accounts.model.DeleteAccountRequest;
import com.nextbreakpoint.shop.accounts.controllers.delete.DeleteAccountRequestMapper;
import com.nextbreakpoint.shop.accounts.model.DeleteAccountResponse;
import com.nextbreakpoint.shop.accounts.controllers.delete.DeleteAccountResponseMapper;
import com.nextbreakpoint.shop.accounts.controllers.delete.DeleteAccountsController;
import com.nextbreakpoint.shop.accounts.model.DeleteAccountsRequest;
import com.nextbreakpoint.shop.accounts.controllers.delete.DeleteAccountsRequestMapper;
import com.nextbreakpoint.shop.accounts.model.DeleteAccountsResponse;
import com.nextbreakpoint.shop.accounts.controllers.delete.DeleteAccountsResponseMapper;
import com.nextbreakpoint.shop.accounts.controllers.insert.InsertAccountController;
import com.nextbreakpoint.shop.accounts.model.InsertAccountRequest;
import com.nextbreakpoint.shop.accounts.controllers.insert.InsertAccountRequestMapper;
import com.nextbreakpoint.shop.accounts.model.InsertAccountResponse;
import com.nextbreakpoint.shop.accounts.controllers.insert.InsertAccountResponseMapper;
import com.nextbreakpoint.shop.accounts.controllers.list.ListAccountsController;
import com.nextbreakpoint.shop.accounts.model.ListAccountsRequest;
import com.nextbreakpoint.shop.accounts.controllers.list.ListAccountsRequestMapper;
import com.nextbreakpoint.shop.accounts.model.ListAccountsResponse;
import com.nextbreakpoint.shop.accounts.controllers.list.ListAccountsResponseMapper;
import com.nextbreakpoint.shop.accounts.controllers.load.LoadAccountController;
import com.nextbreakpoint.shop.accounts.model.LoadAccountRequest;
import com.nextbreakpoint.shop.accounts.controllers.load.LoadAccountRequestMapper;
import com.nextbreakpoint.shop.accounts.model.LoadAccountResponse;
import com.nextbreakpoint.shop.accounts.controllers.load.LoadAccountResponseMapper;
import com.nextbreakpoint.shop.accounts.controllers.load.LoadSelfAccountRequestMapper;
import com.nextbreakpoint.shop.accounts.persistence.CassandraStore;
import com.nextbreakpoint.shop.common.vertx.AccessHandler;
import com.nextbreakpoint.shop.common.cassandra.CassandraClusterFactory;
import com.nextbreakpoint.shop.common.model.Content;
import com.nextbreakpoint.shop.common.handlers.ContentConsumer;
import com.nextbreakpoint.shop.common.handlers.DefaultHandler;
import com.nextbreakpoint.shop.common.handlers.FailedRequestConsumer;
import com.nextbreakpoint.shop.common.model.Failure;
import com.nextbreakpoint.shop.common.graphite.GraphiteManager;
import com.nextbreakpoint.shop.common.vertx.JWTProviderFactory;
import com.nextbreakpoint.shop.common.handlers.NoContentConsumer;
import com.nextbreakpoint.shop.common.vertx.ResponseHelper;
import com.nextbreakpoint.shop.common.vertx.CORSHandlerFactory;
import com.nextbreakpoint.shop.common.vertx.ServerUtil;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Launcher;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.AbstractVerticle;
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
import static com.nextbreakpoint.shop.common.model.Authority.GUEST;
import static com.nextbreakpoint.shop.common.model.Authority.PLATFORM;
import static com.nextbreakpoint.shop.common.model.ContentType.APPLICATION_JSON;
import static com.nextbreakpoint.shop.common.model.Headers.ACCEPT;
import static com.nextbreakpoint.shop.common.model.Headers.AUTHORIZATION;
import static com.nextbreakpoint.shop.common.model.Headers.CONTENT_TYPE;
import static com.nextbreakpoint.shop.common.model.Headers.X_XSRF_TOKEN;
import static com.nextbreakpoint.shop.common.vertx.ServerUtil.UUID_REGEXP;
import static java.util.Arrays.asList;

public class Verticle extends AbstractVerticle {
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
    public void stop(Future<Void> stopFuture) throws Exception {
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

        final CorsHandler corsHandler = CORSHandlerFactory.createWithAll(webUrl, asList(AUTHORIZATION, CONTENT_TYPE, ACCEPT, X_XSRF_TOKEN));

        apiRouter.route("/accounts/*").handler(corsHandler);

        final Handler<RoutingContext> onAccessDenied = rc -> rc.fail(Failure.accessDenied("Authentication failed"));

        final Handler listAccountsHandler = new AccessHandler(jwtProvider, createListAccountsHandler(store), onAccessDenied, asList(ADMIN, PLATFORM));

        final Handler loadSelfAccountHandler = new AccessHandler(jwtProvider, createLoadSelfAccountHandler(store), onAccessDenied, asList(ADMIN, GUEST));

        final Handler loadAccountHandler = new AccessHandler(jwtProvider, createLoadAccountHandler(store), onAccessDenied, asList(ADMIN, PLATFORM));

        final Handler insertAccountHandler = new AccessHandler(jwtProvider, createInsertAccountHandler(store), onAccessDenied, asList(ADMIN, PLATFORM));

        final Handler deleteAccountHandler = new AccessHandler(jwtProvider, createDeleteAccountHandler(store), onAccessDenied, asList(ADMIN));

        final Handler deleteAccountaHandler = new AccessHandler(jwtProvider, createDeleteAccountsHandler(store), onAccessDenied, asList(ADMIN));

        apiRouter.get("/accounts")
                .produces(APPLICATION_JSON)
                .handler(listAccountsHandler);

        apiRouter.get("/accounts/me")
                .produces(APPLICATION_JSON)
                .handler(loadSelfAccountHandler);

        apiRouter.getWithRegex("/accounts/" + UUID_REGEXP)
                .produces(APPLICATION_JSON)
                .handler(loadAccountHandler);

        apiRouter.post("/accounts")
                .produces(APPLICATION_JSON)
                .consumes(APPLICATION_JSON)
                .handler(insertAccountHandler);

        apiRouter.deleteWithRegex("/accounts/" + UUID_REGEXP)
                .produces(APPLICATION_JSON)
                .handler(deleteAccountHandler);

        apiRouter.delete("/accounts")
                .handler(deleteAccountaHandler);

        apiRouter.options("/accounts/*")
                .handler(ResponseHelper::sendNoContent);

        mainRouter.route().failureHandler(ResponseHelper::sendFailure);

        mainRouter.mountSubRouter("/api", apiRouter);

        final HttpServerOptions options = ServerUtil.makeServerOptions(config);

        server = vertx.createHttpServer(options)
                .requestHandler(mainRouter::accept)
                .listen(port);

        return null;
    }

    private DefaultHandler<RoutingContext, DeleteAccountRequest, DeleteAccountResponse, Content> createDeleteAccountHandler(Store store) {
        return DefaultHandler.<RoutingContext, DeleteAccountRequest, DeleteAccountResponse, Content>builder()
                .withInputMapper(new DeleteAccountRequestMapper())
                .withOutputMapper(new DeleteAccountResponseMapper())
                .withController(new DeleteAccountController(store))
                .onSuccess(new ContentConsumer(200))
                .onFailure(new FailedRequestConsumer())
                .build();
    }

    private DefaultHandler<RoutingContext, DeleteAccountsRequest, DeleteAccountsResponse, Content> createDeleteAccountsHandler(Store store) {
        return DefaultHandler.<RoutingContext, DeleteAccountsRequest, DeleteAccountsResponse, Content>builder()
                .withInputMapper(new DeleteAccountsRequestMapper())
                .withOutputMapper(new DeleteAccountsResponseMapper())
                .withController(new DeleteAccountsController(store))
                .onSuccess(new NoContentConsumer(204))
                .onFailure(new FailedRequestConsumer())
                .build();
    }

    private DefaultHandler<RoutingContext, InsertAccountRequest, InsertAccountResponse, Content> createInsertAccountHandler(Store store) {
        return DefaultHandler.<RoutingContext, InsertAccountRequest, InsertAccountResponse, Content>builder()
                .withInputMapper(new InsertAccountRequestMapper())
                .withOutputMapper(new InsertAccountResponseMapper())
                .withController(new InsertAccountController(store))
                .onSuccess(new ContentConsumer(201))
                .onFailure(new FailedRequestConsumer())
                .build();
    }

    private DefaultHandler<RoutingContext, ListAccountsRequest, ListAccountsResponse, Content> createListAccountsHandler(Store store) {
        return DefaultHandler.<RoutingContext, ListAccountsRequest, ListAccountsResponse, Content>builder()
                .withInputMapper(new ListAccountsRequestMapper())
                .withOutputMapper(new ListAccountsResponseMapper())
                .withController(new ListAccountsController(store))
                .onSuccess(new ContentConsumer(200))
                .onFailure(new FailedRequestConsumer())
                .build();
    }

    private DefaultHandler<RoutingContext, LoadAccountRequest, LoadAccountResponse, Content> createLoadAccountHandler(Store store) {
        return DefaultHandler.<RoutingContext, LoadAccountRequest, LoadAccountResponse, Content>builder()
                .withInputMapper(new LoadAccountRequestMapper())
                .withOutputMapper(new LoadAccountResponseMapper())
                .withController(new LoadAccountController(store))
                .onSuccess(new ContentConsumer(200, 404))
                .onFailure(new FailedRequestConsumer())
                .build();
    }

    private DefaultHandler<RoutingContext, LoadAccountRequest, LoadAccountResponse, Content> createLoadSelfAccountHandler(Store store) {
        return DefaultHandler.<RoutingContext, LoadAccountRequest, LoadAccountResponse, Content>builder()
                .withInputMapper(new LoadSelfAccountRequestMapper())
                .withOutputMapper(new LoadAccountResponseMapper())
                .withController(new LoadAccountController(store))
                .onSuccess(new ContentConsumer(200, 404))
                .onFailure(new FailedRequestConsumer())
                .build();
    }
}
