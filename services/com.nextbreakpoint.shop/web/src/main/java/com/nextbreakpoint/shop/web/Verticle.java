package com.nextbreakpoint.shop.web;

import com.nextbreakpoint.shop.common.GraphiteManager;
import com.nextbreakpoint.shop.common.JWTProviderFactory;
import com.nextbreakpoint.shop.common.ResponseHelper;
import com.nextbreakpoint.shop.common.ServerUtil;
import com.nextbreakpoint.shop.common.SimpleTemplateHandler;
import com.nextbreakpoint.shop.common.WebClientFactory;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Launcher;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.http.HttpServer;
import io.vertx.rxjava.ext.auth.jwt.JWTAuth;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.client.WebClient;
import io.vertx.rxjava.ext.web.handler.BodyHandler;
import io.vertx.rxjava.ext.web.handler.CSRFHandler;
import io.vertx.rxjava.ext.web.handler.CookieHandler;
import io.vertx.rxjava.ext.web.handler.LoggerHandler;
import io.vertx.rxjava.ext.web.handler.StaticHandler;
import io.vertx.rxjava.ext.web.handler.TemplateHandler;
import io.vertx.rxjava.ext.web.handler.TimeoutHandler;
import io.vertx.rxjava.ext.web.templ.PebbleTemplateEngine;
import io.vertx.rxjava.ext.web.templ.TemplateEngine;
import rx.Single;

import java.net.MalformedURLException;

import static com.nextbreakpoint.shop.common.ContentType.TEXT_HTML;
import static com.nextbreakpoint.shop.common.ServerUtil.UUID_REGEXP;

public class Verticle extends AbstractVerticle {
    private static final String TEMPLATES = "templates";

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

    private Void createServer(JsonObject config) throws MalformedURLException {
        GraphiteManager.configureMetrics(config);

        final Integer port = config.getInteger("host_port");

        final String secret = config.getString("csrf_secret");

        final TemplateEngine engine = PebbleTemplateEngine.create(vertx);

        final JWTAuth jwtProvider = JWTProviderFactory.create(vertx, config);

        final WebClient designsClient = WebClientFactory.create(vertx, config.getString("server_designs_url"), config);
        final WebClient accountsClient = WebClientFactory.create(vertx, config.getString("server_accounts_url"), config);

        final Router router = Router.router(vertx);

        final TimeoutHandler timeoutHandler = TimeoutHandler.create(30000);

        router.route().handler(LoggerHandler.create());
        router.route().handler(CookieHandler.create());
        router.route().handler(BodyHandler.create());

        router.route("/js/*").handler(timeoutHandler);
        router.route("/css/*").handler(timeoutHandler);
        router.route("/fonts/*").handler(timeoutHandler);
        router.route("/images/*").handler(timeoutHandler);
        router.route("/content/*").handler(timeoutHandler);
        router.route("/admin/*").handler(timeoutHandler);
        router.route("/error/*").handler(timeoutHandler);
        router.route("/config").handler(timeoutHandler);

        router.route("/js/*").handler(StaticHandler.create("js"));
        router.route("/css/*").handler(StaticHandler.create("css"));
        router.route("/fonts/*").handler(StaticHandler.create("fonts"));
        router.route("/images/*").handler(StaticHandler.create("images"));

        router.route("/*").handler(CSRFHandler.create(secret));

        router.route("/*").handler(UserHandler.create(jwtProvider, accountsClient));

        final JsonObject webConfig = new JsonObject();
        webConfig.put("web_url", config.getString("client_web_url"));
        webConfig.put("auth_url", config.getString("client_auth_url"));
        webConfig.put("designs_url", config.getString("client_designs_url"));
        webConfig.put("accounts_url", config.getString("client_accounts_url"));

        router.get("/*").handler(routingContext -> injectConfig(routingContext, webConfig));

        router.get("/config").handler(ConfigHandler.create(webConfig));

        router.getWithRegex("/admin/designs/" + UUID_REGEXP).handler(createPageHandler(engine, "admin/preview"));
        router.get("/admin/designs").handler(createPageHandler(engine, "admin/designs"));

        router.getWithRegex("/content/designs/" + UUID_REGEXP).handler(DesignDataHandler.create(designsClient, config));
        router.get("/content/designs").handler(DesignsDataHandler.create(designsClient, config));

        router.getWithRegex("/content/designs/" + UUID_REGEXP).handler(createPageHandler(engine, "content/preview"));
        router.get("/content/designs").handler(createPageHandler(engine, "content/designs"));

        router.getWithRegex("/watch/([0-9]+)/designs/" + UUID_REGEXP).handler(DesignWatchHandler.create(vertx, jwtProvider, designsClient, config));
        router.getWithRegex("/watch/([0-9]+)/designs").handler(DesignsWatchHandler.create(vertx, jwtProvider, designsClient, config));

        router.get("/error/*").handler(createErrorHandler(engine));

        router.route().failureHandler(routingContext -> ResponseHelper.sendFailure(routingContext));

        server = vertx.createHttpServer(ServerUtil.makeServerOptions(config)).requestHandler(router::accept).listen(port);

        return null;
    }

    private void injectConfig(RoutingContext routingContext, JsonObject webConfig) {
        routingContext.put("admin_url", webConfig.getString("web_url") + "/admin/designs");
        routingContext.put("login_url", webConfig.getString("auth_url") + "/auth/signin/content/designs");
        routingContext.put("logout_url", webConfig.getString("auth_url") + "/auth/signout/content/designs");
        routingContext.put("web_url", webConfig.getString("web_url"));
        routingContext.put("auth_url", webConfig.getString("auth_url"));
        routingContext.put("designs_url", webConfig.getString("designs_url"));
        routingContext.put("accounts_url", webConfig.getString("accounts_url"));
        routingContext.next();
    }

    private TemplateHandler createErrorHandler(TemplateEngine engine) {
        return TemplateHandler.create(engine, TEMPLATES + "/error", TEXT_HTML);
    }

    private Handler<RoutingContext> createPageHandler(TemplateEngine engine, String filename) {
        return SimpleTemplateHandler.create(engine, TEMPLATES, TEXT_HTML, filename);
    }
}
