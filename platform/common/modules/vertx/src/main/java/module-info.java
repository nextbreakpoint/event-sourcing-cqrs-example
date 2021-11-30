module com.nextbreakpoint.blueprint.common.vertx {
    requires static com.nextbreakpoint.blueprint.common.core;
    requires static io.vertx.core;
    requires static io.vertx.auth.common;
    requires static io.vertx.auth.jwt;
    requires static io.vertx.web.client;
    requires static io.vertx.web.openapi;
    requires static io.vertx.client.kafka;
    requires static io.vertx.client.cassandra;
    requires static vertx.rx.java;
    requires static kafka.clients;
    requires static com.datastax.oss.driver.core;
    requires static rxjava;
    requires static lombok;
    exports com.nextbreakpoint.blueprint.common.vertx;
}