module com.nextbreakpoint.blueprint.common.core {
    requires com.fasterxml.jackson.annotation;
    exports com.nextbreakpoint.blueprint.common.core;
    opens com.nextbreakpoint.blueprint.common.core to com.fasterxml.jackson.databind;
}