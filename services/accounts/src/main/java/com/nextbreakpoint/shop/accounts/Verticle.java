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
import com.nextbreakpoint.shop.common.CassandraMigrationManager;
import com.nextbreakpoint.shop.common.ContentHandler;
import com.nextbreakpoint.shop.common.DelegateHandler;
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
import static com.nextbreakpoint.shop.common.Headers.XSRFTOKEN;
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
        CassandraMigrationManager.migrateDatabase(config);

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

        final CorsHandler corsHandler = CORSHandlerFactory.createWithAll(webUrl, asList(AUTHORIZATION, CONTENT_TYPE, ACCEPT, XSRFTOKEN));

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

    private DelegateHandler<DeleteAccountRequest, DeleteAccountResponse> createDeleteAccountHandler(Store store) {
        return DelegateHandler.<DeleteAccountRequest, DeleteAccountResponse>builder()
                .with(new DeleteAccountRequestMapper())
                .with(new DeleteAccountResponseMapper())
                .with(new DeleteAccountController(store))
                .with(new ContentHandler(200))
                .with(new FailedRequestHandler())
                .build();
    }

    private DelegateHandler<DeleteAccountsRequest, DeleteAccountsResponse> createDeleteAccountsHandler(Store store) {
        return DelegateHandler.<DeleteAccountsRequest, DeleteAccountsResponse>builder()
                .with(new DeleteAccountsRequestMapper())
                .with(new DeleteAccountsResponseMapper())
                .with(new DeleteAccountsController(store))
                .with(new NoContentHandler(204))
                .with(new FailedRequestHandler())
                .build();
    }

    private DelegateHandler<InsertAccountRequest, InsertAccountResponse> createInsertAccountHandler(Store store) {
        return DelegateHandler.<InsertAccountRequest, InsertAccountResponse>builder()
                .with(new InsertAccountRequestMapper())
                .with(new InsertAccountResponseMapper())
                .with(new InsertAccountController(store))
                .with(new ContentHandler(201))
                .with(new FailedRequestHandler())
                .build();
    }

    private DelegateHandler<ListAccountsRequest, ListAccountsResponse> createListAccountsHandler(Store store) {
        return DelegateHandler.<ListAccountsRequest, ListAccountsResponse>builder()
                .with(new ListAccountsRequestMapper())
                .with(new ListAccountsResponseMapper())
                .with(new ListAccountsController(store))
                .with(new ContentHandler(200))
                .with(new FailedRequestHandler())
                .build();
    }

    private DelegateHandler<LoadAccountRequest, LoadAccountResponse> createLoadAccountHandler(Store store) {
        return DelegateHandler.<LoadAccountRequest, LoadAccountResponse>builder()
                .with(new LoadAccountRequestMapper())
                .with(new LoadAccountResponseMapper())
                .with(new LoadAccountController(store))
                .with(new ContentHandler(200, 404))
                .with(new FailedRequestHandler())
                .build();
    }

    private DelegateHandler<LoadAccountRequest, LoadAccountResponse> createLoadSelfAccountHandler(Store store) {
        return DelegateHandler.<LoadAccountRequest, LoadAccountResponse>builder()
                .with(new LoadSelfAccountRequestMapper())
                .with(new LoadAccountResponseMapper())
                .with(new LoadAccountController(store))
                .with(new ContentHandler(200, 404))
                .with(new FailedRequestHandler())
                .build();
    }
}
