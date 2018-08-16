package com.nextbreakpoint.shop.accounts;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.nextbreakpoint.shop.accounts.delete.DeleteAccountController;
import com.nextbreakpoint.shop.accounts.delete.DeleteAccountRequest;
import com.nextbreakpoint.shop.accounts.delete.DeleteAccountRequestMapper;
import com.nextbreakpoint.shop.accounts.delete.DeleteAccountResponse;
import com.nextbreakpoint.shop.accounts.delete.DeleteAccountResponseMapper;
import com.nextbreakpoint.shop.accounts.delete.DeleteAccountsController;
import com.nextbreakpoint.shop.accounts.delete.DeleteAccountsRequest;
import com.nextbreakpoint.shop.accounts.delete.DeleteAccountsRequestMapper;
import com.nextbreakpoint.shop.accounts.delete.DeleteAccountsResponse;
import com.nextbreakpoint.shop.accounts.delete.DeleteAccountsResponseMapper;
import com.nextbreakpoint.shop.accounts.insert.InsertAccountController;
import com.nextbreakpoint.shop.accounts.insert.InsertAccountRequest;
import com.nextbreakpoint.shop.accounts.insert.InsertAccountRequestMapper;
import com.nextbreakpoint.shop.accounts.insert.InsertAccountResponse;
import com.nextbreakpoint.shop.accounts.insert.InsertAccountResponseMapper;
import com.nextbreakpoint.shop.accounts.list.ListAccountsController;
import com.nextbreakpoint.shop.accounts.list.ListAccountsRequest;
import com.nextbreakpoint.shop.accounts.list.ListAccountsRequestMapper;
import com.nextbreakpoint.shop.accounts.list.ListAccountsResponse;
import com.nextbreakpoint.shop.accounts.list.ListAccountsResponseMapper;
import com.nextbreakpoint.shop.accounts.load.LoadAccountController;
import com.nextbreakpoint.shop.accounts.load.LoadAccountRequest;
import com.nextbreakpoint.shop.accounts.load.LoadAccountRequestMapper;
import com.nextbreakpoint.shop.accounts.load.LoadAccountResponse;
import com.nextbreakpoint.shop.accounts.load.LoadAccountResponseMapper;
import com.nextbreakpoint.shop.accounts.load.LoadSelfAccountRequestMapper;
import com.nextbreakpoint.shop.common.AccessHandler;
import com.nextbreakpoint.shop.common.CassandraClusterFactory;
import com.nextbreakpoint.shop.common.ContentHandler;
import com.nextbreakpoint.shop.common.RESTContentHandler;
import com.nextbreakpoint.shop.common.FailedRequestHandler;
import com.nextbreakpoint.shop.common.Failure;
import com.nextbreakpoint.shop.common.GraphiteManager;
import com.nextbreakpoint.shop.common.JWTProviderFactory;
import com.nextbreakpoint.shop.common.NoContentHandler;
import com.nextbreakpoint.shop.common.ResponseHelper;
import com.nextbreakpoint.shop.common.CORSHandlerFactory;
import com.nextbreakpoint.shop.common.ServerUtil;
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

import static com.nextbreakpoint.shop.common.Authority.ADMIN;
import static com.nextbreakpoint.shop.common.Authority.GUEST;
import static com.nextbreakpoint.shop.common.Authority.PLATFORM;
import static com.nextbreakpoint.shop.common.ContentType.APPLICATION_JSON;
import static com.nextbreakpoint.shop.common.Headers.ACCEPT;
import static com.nextbreakpoint.shop.common.Headers.AUTHORIZATION;
import static com.nextbreakpoint.shop.common.Headers.CONTENT_TYPE;
import static com.nextbreakpoint.shop.common.Headers.X_XSRF_TOKEN;
import static com.nextbreakpoint.shop.common.ServerUtil.UUID_REGEXP;
import static java.util.Arrays.asList;

public class Verticle extends AbstractVerticle {
    private HttpServer server;

    public static void main(String[] args) {
        System.setProperty("vertx.metrics.options.enabled", "true");
        System.setProperty("vertx.metrics.options.registryName", "exported");

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

    private RESTContentHandler<DeleteAccountRequest, DeleteAccountResponse> createDeleteAccountHandler(Store store) {
        return RESTContentHandler.<DeleteAccountRequest, DeleteAccountResponse>builder()
                .withInputMapper(new DeleteAccountRequestMapper())
                .withOutputMapper(new DeleteAccountResponseMapper())
                .withController(new DeleteAccountController(store))
                .onSuccess(new ContentHandler(200))
                .onFailure(new FailedRequestHandler())
                .build();
    }

    private RESTContentHandler<DeleteAccountsRequest, DeleteAccountsResponse> createDeleteAccountsHandler(Store store) {
        return RESTContentHandler.<DeleteAccountsRequest, DeleteAccountsResponse>builder()
                .withInputMapper(new DeleteAccountsRequestMapper())
                .withOutputMapper(new DeleteAccountsResponseMapper())
                .withController(new DeleteAccountsController(store))
                .onSuccess(new NoContentHandler(204))
                .onFailure(new FailedRequestHandler())
                .build();
    }

    private RESTContentHandler<InsertAccountRequest, InsertAccountResponse> createInsertAccountHandler(Store store) {
        return RESTContentHandler.<InsertAccountRequest, InsertAccountResponse>builder()
                .withInputMapper(new InsertAccountRequestMapper())
                .withOutputMapper(new InsertAccountResponseMapper())
                .withController(new InsertAccountController(store))
                .onSuccess(new ContentHandler(201))
                .onFailure(new FailedRequestHandler())
                .build();
    }

    private RESTContentHandler<ListAccountsRequest, ListAccountsResponse> createListAccountsHandler(Store store) {
        return RESTContentHandler.<ListAccountsRequest, ListAccountsResponse>builder()
                .withInputMapper(new ListAccountsRequestMapper())
                .withOutputMapper(new ListAccountsResponseMapper())
                .withController(new ListAccountsController(store))
                .onSuccess(new ContentHandler(200))
                .onFailure(new FailedRequestHandler())
                .build();
    }

    private RESTContentHandler<LoadAccountRequest, LoadAccountResponse> createLoadAccountHandler(Store store) {
        return RESTContentHandler.<LoadAccountRequest, LoadAccountResponse>builder()
                .withInputMapper(new LoadAccountRequestMapper())
                .withOutputMapper(new LoadAccountResponseMapper())
                .withController(new LoadAccountController(store))
                .onSuccess(new ContentHandler(200, 404))
                .onFailure(new FailedRequestHandler())
                .build();
    }

    private RESTContentHandler<LoadAccountRequest, LoadAccountResponse> createLoadSelfAccountHandler(Store store) {
        return RESTContentHandler.<LoadAccountRequest, LoadAccountResponse>builder()
                .withInputMapper(new LoadSelfAccountRequestMapper())
                .withOutputMapper(new LoadAccountResponseMapper())
                .withController(new LoadAccountController(store))
                .onSuccess(new ContentHandler(200, 404))
                .onFailure(new FailedRequestHandler())
                .build();
    }
}
