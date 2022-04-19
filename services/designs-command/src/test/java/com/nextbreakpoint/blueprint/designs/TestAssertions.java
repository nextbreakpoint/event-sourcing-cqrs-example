package com.nextbreakpoint.blueprint.designs;

import com.datastax.oss.driver.api.core.cql.Row;
import com.nextbreakpoint.blueprint.common.commands.DesignDeleteCommand;
import com.nextbreakpoint.blueprint.common.commands.DesignInsertCommand;
import com.nextbreakpoint.blueprint.common.commands.DesignUpdateCommand;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.events.DesignDeleteRequested;
import com.nextbreakpoint.blueprint.common.events.DesignInsertRequested;
import com.nextbreakpoint.blueprint.common.events.DesignUpdateRequested;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class TestAssertions {
    private TestAssertions() {}

    public static void assertExpectedDesignInsertRequestedMessage(InputMessage actualMessage, String designId) {
        assertThat(actualMessage.getTimestamp()).isNotNull();
        assertThat(actualMessage.getKey()).isEqualTo(designId);
        assertThat(actualMessage.getToken()).isNotNull();
        assertThat(actualMessage.getValue()).isNotNull();
        assertThat(actualMessage.getValue().getSource()).isEqualTo(TestConstants.MESSAGE_SOURCE);
        assertThat(actualMessage.getValue().getUuid()).isNotNull();
        assertThat(actualMessage.getValue().getType()).isEqualTo(TestConstants.DESIGN_INSERT_REQUESTED);
        assertThat(actualMessage.getValue()).isNotNull();
        DesignInsertRequested actualEvent = Json.decodeValue(actualMessage.getValue().getData(), DesignInsertRequested.class);
        assertThat(actualEvent.getUserId()).isEqualTo(TestConstants.USER_ID);
        assertThat(actualEvent.getCommandId()).isNotNull();
        assertThat(actualEvent.getDesignId()).isEqualTo(UUID.fromString(designId));
        assertThat(actualEvent.getData()).isNotNull();
        Design decodedDesign = Json.decodeValue(actualEvent.getData(), Design.class);
        assertThat(decodedDesign.getManifest()).isEqualTo(TestConstants.MANIFEST);
        assertThat(decodedDesign.getMetadata()).isEqualTo(TestConstants.METADATA);
        assertThat(decodedDesign.getScript()).isEqualTo(TestConstants.SCRIPT);
    }

    public static void assertExpectedDesignUpdateRequestedMessage(InputMessage actualMessage, String designId) {
        assertThat(actualMessage.getTimestamp()).isNotNull();
        assertThat(actualMessage.getKey()).isEqualTo(designId);
        assertThat(actualMessage.getToken()).isNotNull();
        assertThat(actualMessage.getValue()).isNotNull();
        assertThat(actualMessage.getValue().getSource()).isEqualTo(TestConstants.MESSAGE_SOURCE);
        assertThat(actualMessage.getValue().getUuid()).isNotNull();
        assertThat(actualMessage.getValue().getType()).isEqualTo(TestConstants.DESIGN_UPDATE_REQUESTED);
        assertThat(actualMessage.getValue()).isNotNull();
        DesignUpdateRequested actualEvent = Json.decodeValue(actualMessage.getValue().getData(), DesignUpdateRequested.class);
        assertThat(actualEvent.getUserId()).isEqualTo(TestConstants.USER_ID);
        assertThat(actualEvent.getCommandId()).isNotNull();
        assertThat(actualEvent.getDesignId()).isEqualTo(UUID.fromString(designId));
        assertThat(actualEvent.getPublished()).isEqualTo(false);
        assertThat(actualEvent.getData()).isNotNull();
        Design decodedDesign = Json.decodeValue(actualEvent.getData(), Design.class);
        assertThat(decodedDesign.getManifest()).isEqualTo(TestConstants.MANIFEST);
        assertThat(decodedDesign.getMetadata()).isEqualTo(TestConstants.METADATA);
        assertThat(decodedDesign.getScript()).isEqualTo(TestConstants.SCRIPT);
    }

    public static void assertExpectedDesignDeleteRequestedMessage(InputMessage actualMessage, String designId) {
        assertThat(actualMessage.getTimestamp()).isNotNull();
        assertThat(actualMessage.getKey()).isEqualTo(designId);
        assertThat(actualMessage.getToken()).isNotNull();
        assertThat(actualMessage.getValue()).isNotNull();
        assertThat(actualMessage.getValue().getSource()).isEqualTo(TestConstants.MESSAGE_SOURCE);
        assertThat(actualMessage.getValue().getUuid()).isNotNull();
        assertThat(actualMessage.getValue().getType()).isEqualTo(TestConstants.DESIGN_DELETE_REQUESTED);
        assertThat(actualMessage.getValue()).isNotNull();
        DesignDeleteRequested actualEvent = Json.decodeValue(actualMessage.getValue().getData(), DesignDeleteRequested.class);
        assertThat(actualEvent.getUserId()).isEqualTo(TestConstants.USER_ID);
        assertThat(actualEvent.getCommandId()).isNotNull();
        assertThat(actualEvent.getDesignId()).isEqualTo(UUID.fromString(designId));
    }

    public static void assertExpectedDesignInsertCommand(Row row, String uuid) {
        final String actualType = row.getString("MESSAGE_TYPE");
        final String actualValue = row.getString("MESSAGE_VALUE");
        final UUID actualUuid = row.getUuid("MESSAGE_UUID");
        final String actualToken = row.getString("MESSAGE_TOKEN");
        final String actualSource = row.getString("MESSAGE_SOURCE");
        final String actualKey = row.getString("MESSAGE_KEY");
        final Instant actualTimestamp = row.getInstant("MESSAGE_TIMESTAMP");
        assertThat(actualUuid).isNotNull();
        assertThat(actualToken).isNotNull();
        assertThat(actualValue).isNotNull();
        assertThat(actualType).isEqualTo(DesignInsertCommand.TYPE);
        assertThat(actualSource).isNotNull();
        assertThat(actualKey).isEqualTo(uuid);
        assertThat(actualTimestamp).isNotNull();
    }

    public static void assertExpectedDesignUpdateCommand(Row row, String uuid) {
        final String actualType = row.getString("MESSAGE_TYPE");
        final String actualValue = row.getString("MESSAGE_VALUE");
        final UUID actualUuid = row.getUuid("MESSAGE_UUID");
        final String actualToken = row.getString("MESSAGE_TOKEN");
        final String actualSource = row.getString("MESSAGE_SOURCE");
        final String actualKey = row.getString("MESSAGE_KEY");
        final Instant actualTimestamp = row.getInstant("MESSAGE_TIMESTAMP");
        assertThat(actualUuid).isNotNull();
        assertThat(actualToken).isNotNull();
        assertThat(actualValue).isNotNull();
        assertThat(actualType).isEqualTo(DesignUpdateCommand.TYPE);
        assertThat(actualSource).isNotNull();
        assertThat(actualKey).isEqualTo(uuid);
        assertThat(actualTimestamp).isNotNull();
    }

    public static void assertExpectedDesignDeleteCommand(Row row, String uuid) {
        final String actualType = row.getString("MESSAGE_TYPE");
        final String actualValue = row.getString("MESSAGE_VALUE");
        final UUID actualUuid = row.getUuid("MESSAGE_UUID");
        final String actualToken = row.getString("MESSAGE_TOKEN");
        final String actualSource = row.getString("MESSAGE_SOURCE");
        final String actualKey = row.getString("MESSAGE_KEY");
        final Instant actualTimestamp = row.getInstant("MESSAGE_TIMESTAMP");
        assertThat(actualUuid).isNotNull();
        assertThat(actualToken).isNotNull();
        assertThat(actualValue).isNotNull();
        assertThat(actualType).isEqualTo(DesignDeleteCommand.TYPE);
        assertThat(actualSource).isNotNull();
        assertThat(actualKey).isEqualTo(uuid);
        assertThat(actualTimestamp).isNotNull();
    }
}
