module com.nextbreakpoint.blueprint.accounts {
    requires com.nextbreakpoint.blueprint.common.core;
    requires com.nextbreakpoint.blueprint.common.vertx;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    opens com.nextbreakpoint.blueprint.accounts to com.nextbreakpoint.blueprint.common.vertx;
}