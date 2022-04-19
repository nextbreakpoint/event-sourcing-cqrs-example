module com.nextbreakpoint.blueprint.common.events {
    requires static com.fasterxml.jackson.annotation;
    requires static com.fasterxml.jackson.databind;
    requires static com.nextbreakpoint.blueprint.common.core;
    requires static org.apache.logging.log4j;
    requires static io.vertx.core;
    requires static lombok;
    exports com.nextbreakpoint.blueprint.common.events;
    exports com.nextbreakpoint.blueprint.common.events.mappers;
    opens com.nextbreakpoint.blueprint.common.events to com.fasterxml.jackson.databind;
}