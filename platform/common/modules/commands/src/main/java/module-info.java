module com.nextbreakpoint.blueprint.common.commands {
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;
    requires com.nextbreakpoint.blueprint.common.core;
    requires io.vertx.core;
    requires static lombok;
    exports com.nextbreakpoint.blueprint.common.commands;
    exports com.nextbreakpoint.blueprint.common.commands.mappers;
    opens com.nextbreakpoint.blueprint.common.commands to com.fasterxml.jackson.databind;
}