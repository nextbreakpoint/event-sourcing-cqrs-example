module com.nextbreakpoint.blueprint.common.commands {
    requires static com.fasterxml.jackson.annotation;
    requires static com.fasterxml.jackson.databind;
    requires static com.nextbreakpoint.blueprint.common.core;
    requires static org.apache.logging.log4j;
    requires static io.vertx.core;
    requires static lombok;
    exports com.nextbreakpoint.blueprint.common.commands;
    exports com.nextbreakpoint.blueprint.common.commands.mappers;
    opens com.nextbreakpoint.blueprint.common.commands to com.fasterxml.jackson.databind;
}