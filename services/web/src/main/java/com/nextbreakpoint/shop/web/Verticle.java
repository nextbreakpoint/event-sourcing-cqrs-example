package com.nextbreakpoint.shop.web;

import com.nextbreakpoint.shop.common.graphite.GraphiteManager;
import com.nextbreakpoint.shop.common.vertx.JWTProviderFactory;
import com.nextbreakpoint.shop.common.vertx.ResponseHelper;
import com.nextbreakpoint.shop.common.vertx.ServerUtil;
import com.nextbreakpoint.shop.common.vertx.SimpleTemplateHandler;
import com.nextbreakpoint.shop.common.vertx.WebClientFactory;
import com.nextbreakpoint.shop.web.handlers.ConfigHandler;
import com.nextbreakpoint.shop.web.handlers.DesignDataHandler;
import com.nextbreakpoint.shop.web.handlers.DesignsDataHandler;
import com.nextbreakpoint.shop.web.handlers.UserHandler;
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

import static com.nextbreakpoint.shop.common.model.ContentType.TEXT_HTML;
import static com.nextbreakpoint.shop.common.vertx.ServerUtil.UUID_REGEXP;

public class Verticle extends AbstractVerticle {
    private static final String TEMPLATES = "templates";

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

    private Void createServer(JsonObject config) throws MalformedURLException {
        GraphiteManager.configureMetrics(config);

        final Integer port = config.getInteger("host_port");

        final String secret = config.getString("csrf_secret");

        final TemplateEngine engine = PebbleTemplateEngine.create(vertx);

        final JWTAuth jwtProvider = JWTProviderFactory.create(vertx, config);

        final WebClient designsClient = WebClientFactory.create(vertx, config.getString("server_designs_url"), config);
        final WebClient accountsClient = WebClientFactory.create(vertx, config.getString("server_accounts_url"), config);

        final Router mainRouter = Router.router(vertx);

        final TimeoutHandler timeoutHandler = TimeoutHandler.create(30000);

        mainRouter.route().handler(LoggerHandler.create());
        mainRouter.route().handler(CookieHandler.create());
        mainRouter.route().handler(BodyHandler.create());

        mainRouter.route("/js/*").handler(timeoutHandler);
        mainRouter.route("/css/*").handler(timeoutHandler);
        mainRouter.route("/fonts/*").handler(timeoutHandler);
        mainRouter.route("/images/*").handler(timeoutHandler);
        mainRouter.route("/content/*").handler(timeoutHandler);
        mainRouter.route("/admin/*").handler(timeoutHandler);
        mainRouter.route("/error/*").handler(timeoutHandler);
        mainRouter.route("/config").handler(timeoutHandler);

        mainRouter.route("/js/*").handler(StaticHandler.create("js"));
        mainRouter.route("/css/*").handler(StaticHandler.create("css"));
        mainRouter.route("/fonts/*").handler(StaticHandler.create("fonts"));
        mainRouter.route("/images/*").handler(StaticHandler.create("images"));

        mainRouter.route("/*").handler(CSRFHandler.create(secret));

        mainRouter.route("/*").handler(UserHandler.create(jwtProvider, accountsClient));

        final JsonObject webConfig = new JsonObject()
                .put("web_url", config.getString("client_web_url"))
                .put("auth_url", config.getString("client_auth_url"))
                .put("designs_url", config.getString("client_designs_url"))
                .put("accounts_url", config.getString("client_accounts_url"))
                .put("designs_processor_url", config.getString("client_designs_processor_url"))
                .put("designs_command_url", config.getString("client_designs_command_url"))
                .put("designs_query_url", config.getString("client_designs_query_url"))
                .put("designs_sse_url", config.getString("client_designs_sse_url"));

        mainRouter.get("/*").handler(routingContext -> injectConfig(routingContext, webConfig));

        mainRouter.get("/config").handler(ConfigHandler.create(webConfig));

        mainRouter.getWithRegex("/admin/designs/" + UUID_REGEXP)
                .handler(createPageHandler(engine, "admin/preview"));

        mainRouter.get("/admin/designs")
                .handler(createPageHandler(engine, "admin/designs"));

        mainRouter.getWithRegex("/content/designs/" + UUID_REGEXP)
                .handler(DesignDataHandler.create(designsClient, config));

        mainRouter.getWithRegex("/content/designs/" + UUID_REGEXP)
                .handler(createPageHandler(engine, "content/preview"));

        mainRouter.get("/content/designs")
                .handler(DesignsDataHandler.create(designsClient, config));

        mainRouter.get("/content/designs")
                .handler(createPageHandler(engine, "content/designs"));

        mainRouter.get("/error/*").handler(createErrorHandler(engine));

        mainRouter.route().failureHandler(ResponseHelper::sendFailure);

        final HttpServerOptions options = ServerUtil.makeServerOptions(config);

        server = vertx.createHttpServer(options)
                .requestHandler(mainRouter::accept)
                .listen(port);

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
        routingContext.put("designs_processor_url", webConfig.getString("designs_processor_url"));
        routingContext.put("designs_command_url", webConfig.getString("designs_command_url"));
        routingContext.put("designs_query_url", webConfig.getString("designs_query_url"));
        routingContext.put("designs_sse_url", webConfig.getString("designs_sse_url"));
        routingContext.next();
    }

    private TemplateHandler createErrorHandler(TemplateEngine engine) {
        return TemplateHandler.create(engine, TEMPLATES + "/error", TEXT_HTML);
    }

    private Handler<RoutingContext> createPageHandler(TemplateEngine engine, String filename) {
        return SimpleTemplateHandler.create(engine, TEMPLATES, TEXT_HTML, filename);
    }
}
