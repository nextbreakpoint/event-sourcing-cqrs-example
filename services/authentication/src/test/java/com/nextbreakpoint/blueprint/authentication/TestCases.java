package com.nextbreakpoint.blueprint.authentication;

import io.vertx.rxjava.core.RxHelper;
import io.vertx.rxjava.core.Vertx;
import org.jetbrains.annotations.NotNull;
import rx.plugins.RxJavaHooks;
import rx.schedulers.Schedulers;

import java.net.MalformedURLException;
import java.net.URL;

public class TestCases {
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
}
