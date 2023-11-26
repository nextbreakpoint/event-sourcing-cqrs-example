module com.nextbreakpoint.blueprint.common.events {
    requires static org.apache.avro;
    requires static lombok;
    exports com.nextbreakpoint.blueprint.common.events.avro;
    opens com.nextbreakpoint.blueprint.common.events.avro to org.apache.avro;
}