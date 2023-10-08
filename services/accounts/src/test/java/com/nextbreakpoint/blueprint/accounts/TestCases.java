package com.nextbreakpoint.blueprint.accounts;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import io.vertx.rxjava.core.RxHelper;
import io.vertx.rxjava.core.Vertx;
import org.jetbrains.annotations.NotNull;
import rx.plugins.RxJavaHooks;
import rx.schedulers.Schedulers;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;

public class TestCases {
    private final AtomicInteger counter = new AtomicInteger(10);

    private final TestScenario scenario = new TestScenario();

    private final Vertx vertx = new Vertx(io.vertx.core.Vertx.vertx());

    public TestCases() {}

    public void before() {
        scenario.before();

        RxJavaHooks.setOnComputationScheduler(s -> RxHelper.scheduler(vertx));
        RxJavaHooks.setOnIOScheduler(s -> RxHelper.blockingScheduler(vertx));
    }

    public void after() {
        try {
            vertx.rxClose()
                    .doOnError(Throwable::printStackTrace)
                    .subscribeOn(Schedulers.io())
                    .toCompletable()
                    .await();
        } catch (Exception ignore) {
        }

        scenario.after();
    }

    @NotNull
    public String getVersion() {
        return scenario.getVersion();
    }

    @NotNull
    public URL makeBaseURL(String path) throws MalformedURLException {
        final String normPath = path.startsWith("/") ? path.substring(1) : path;
        return new URL("http://" + scenario.getServiceHost() + ":" + scenario.getServicePort() + "/" + normPath);
    }

    @NotNull
    public String makeAuthorization(String user, String role) {
        return scenario.makeAuthorization(user, role);
    }

    @NotNull
    public String getOriginUrl() {
        return "http://" + scenario.getServiceHost() + ":" + scenario.getServicePort();
    }

    @NotNull
    public String makeUniqueEmail() {
        return "user" + counter.getAndIncrement() + "@localhost";
    }

    @NotNull
    public HttpTestTarget getHttpTestTarget() {
        return new HttpTestTarget(scenario.getServiceHost(), scenario.getServicePort(), "/");
    }

    @NotNull
    public String getMySqlConnectionUrl(String db) {
        return String.format("jdbc:mysql://%s:%s/%s", scenario.getMySQLHost(), scenario.getMySQLPort(), db);
    }
}
