module com.nextbreakpoint.blueprint.designscommandconsumer {
    requires com.nextbreakpoint.blueprint.common.core;
    requires com.nextbreakpoint.blueprint.common.vertx;
    requires com.nextbreakpoint.blueprint.common.cassandra;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    opens com.nextbreakpoint.blueprint.designs to com.nextbreakpoint.blueprint.common.vertx;
}