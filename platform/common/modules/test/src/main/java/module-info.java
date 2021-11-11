module com.nextbreakpoint.blueprint.common.test {
    requires static com.nextbreakpoint.blueprint.common.core;
    requires static com.nextbreakpoint.blueprint.common.vertx;
    requires static rest.assured;
    requires static restito;
    requires static awaitility;
    requires static io.vertx.core;
    requires static vertx.rx.java;
    requires static io.vertx.auth.jwt;
    requires static io.vertx.auth.common;
    requires static kafka.clients;
    requires static rxjava;
    requires io.vertx.client.kafka;
    exports com.nextbreakpoint.blueprint.common.test;
}