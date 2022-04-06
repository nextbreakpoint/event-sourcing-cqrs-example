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
    requires static io.vertx.client.kafka;
    requires static kafka.clients;
    requires static com.datastax.oss.driver.core;
    requires static rxjava;
    requires static testcontainers;
    requires static org.apache.logging.log4j;
    exports com.nextbreakpoint.blueprint.common.test;
}