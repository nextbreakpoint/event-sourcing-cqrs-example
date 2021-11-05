module com.nextbreakpoint.blueprint.common.core {
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;
    exports com.nextbreakpoint.blueprint.common.core;
    opens com.nextbreakpoint.blueprint.common.core to com.fasterxml.jackson.databind;
}