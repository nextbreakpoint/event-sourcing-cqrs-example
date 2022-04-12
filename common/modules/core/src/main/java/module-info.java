module com.nextbreakpoint.blueprint.common.core {
    requires static com.fasterxml.jackson.annotation;
    requires static com.fasterxml.jackson.databind;
    requires static com.fasterxml.jackson.datatype.jsr310;
    requires static rxjava;
    requires static org.apache.logging.log4j;
    requires static lombok;
    exports com.nextbreakpoint.blueprint.common.core;
    opens com.nextbreakpoint.blueprint.common.core to com.fasterxml.jackson.databind;
}