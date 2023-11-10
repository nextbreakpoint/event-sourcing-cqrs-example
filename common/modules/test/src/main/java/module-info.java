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
    requires static org.apache.avro;
    requires static com.datastax.oss.driver.core;
    requires static rxjava;
    requires static testcontainers;
    requires static org.apache.logging.log4j;
    requires static com.fasterxml.jackson.core;
    requires static com.fasterxml.jackson.databind;
    requires static com.fasterxml.jackson.annotation;
    exports com.nextbreakpoint.blueprint.common.test;
}