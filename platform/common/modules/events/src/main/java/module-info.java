module com.nextbreakpoint.blueprint.common.events {
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;
    requires com.nextbreakpoint.blueprint.common.core;
    requires io.vertx.core;
    requires static lombok;
    exports com.nextbreakpoint.blueprint.common.events;
    exports com.nextbreakpoint.blueprint.common.events.mappers;
    opens com.nextbreakpoint.blueprint.common.events to com.fasterxml.jackson.databind;
}