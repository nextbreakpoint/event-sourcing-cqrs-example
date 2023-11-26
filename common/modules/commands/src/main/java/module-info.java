module com.nextbreakpoint.blueprint.common.commands {
    requires static org.apache.avro;
    requires static lombok;
    exports com.nextbreakpoint.blueprint.common.commands.avro;
    opens com.nextbreakpoint.blueprint.common.commands.avro to org.apache.avro;
}