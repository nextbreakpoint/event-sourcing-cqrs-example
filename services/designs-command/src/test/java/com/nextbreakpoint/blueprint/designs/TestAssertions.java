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
import org.assertj.core.api.SoftAssertions;

import java.time.Instant;
import java.util.UUID;

import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_DELETE_REQUESTED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_INSERT_REQUESTED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_UPDATE_REQUESTED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.MANIFEST;
import static com.nextbreakpoint.blueprint.designs.TestConstants.MESSAGE_SOURCE;
import static com.nextbreakpoint.blueprint.designs.TestConstants.METADATA;
import static com.nextbreakpoint.blueprint.designs.TestConstants.SCRIPT;

public class TestAssertions {
    private TestAssertions() {}

    public static void assertExpectedDesignInsertRequestedMessage(InputMessage actualMessage, String designId) {
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(actualMessage.getTimestamp()).isNotNull();
        softly.assertThat(actualMessage.getKey()).isEqualTo(designId);
        softly.assertThat(actualMessage.getToken()).isNotNull();
        softly.assertThat(actualMessage.getValue()).isNotNull();
        softly.assertThat(actualMessage.getValue().getSource()).isEqualTo(MESSAGE_SOURCE);
        softly.assertThat(actualMessage.getValue().getUuid()).isNotNull();
        softly.assertThat(actualMessage.getValue().getType()).isEqualTo(DESIGN_INSERT_REQUESTED);
        softly.assertThat(actualMessage.getValue()).isNotNull();
        softly.assertAll();
    }

    public static void assertExpectedDesignUpdateRequestedMessage(InputMessage actualMessage, String designId) {
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(actualMessage.getTimestamp()).isNotNull();
        softly.assertThat(actualMessage.getKey()).isEqualTo(designId);
        softly.assertThat(actualMessage.getToken()).isNotNull();
        softly.assertThat(actualMessage.getValue()).isNotNull();
        softly.assertThat(actualMessage.getValue().getSource()).isEqualTo(MESSAGE_SOURCE);
        softly.assertThat(actualMessage.getValue().getUuid()).isNotNull();
        softly.assertThat(actualMessage.getValue().getType()).isEqualTo(DESIGN_UPDATE_REQUESTED);
        softly.assertThat(actualMessage.getValue()).isNotNull();
        softly.assertAll();
    }

    public static void assertExpectedDesignDeleteRequestedMessage(InputMessage actualMessage, String designId) {
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(actualMessage.getTimestamp()).isNotNull();
        softly.assertThat(actualMessage.getKey()).isEqualTo(designId);
        softly.assertThat(actualMessage.getToken()).isNotNull();
        softly.assertThat(actualMessage.getValue()).isNotNull();
        softly.assertThat(actualMessage.getValue().getSource()).isEqualTo(MESSAGE_SOURCE);
        softly.assertThat(actualMessage.getValue().getUuid()).isNotNull();
        softly.assertThat(actualMessage.getValue().getType()).isEqualTo(DESIGN_DELETE_REQUESTED);
        softly.assertThat(actualMessage.getValue()).isNotNull();
        softly.assertAll();
    }

    public static void assertExpectedDesignInsertRequestedEvent(DesignInsertRequested actualEvent, UUID designId, UUID userId) {
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(actualEvent.getUserId()).isEqualTo(userId);
        softly.assertThat(actualEvent.getCommandId()).isNotNull();
        softly.assertThat(actualEvent.getDesignId()).isEqualTo(designId);
        softly.assertThat(actualEvent.getData()).isNotNull();
        Design decodedDesign = Json.decodeValue(actualEvent.getData(), Design.class);
        softly.assertThat(decodedDesign.getManifest()).isEqualTo(MANIFEST);
        softly.assertThat(decodedDesign.getMetadata()).isEqualTo(METADATA);
        softly.assertThat(decodedDesign.getScript()).isEqualTo(SCRIPT);
        softly.assertAll();
    }

    public static void assertExpectedDesignUpdateRequestedEvent(DesignUpdateRequested actualEvent, UUID designId, UUID userId) {
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(actualEvent.getUserId()).isEqualTo(userId);
        softly.assertThat(actualEvent.getCommandId()).isNotNull();
        softly.assertThat(actualEvent.getDesignId()).isEqualTo(designId);
        softly.assertThat(actualEvent.getPublished()).isEqualTo(false);
        softly.assertThat(actualEvent.getData()).isNotNull();
        Design decodedDesign = Json.decodeValue(actualEvent.getData(), Design.class);
        softly.assertThat(decodedDesign.getManifest()).isEqualTo(MANIFEST);
        softly.assertThat(decodedDesign.getMetadata()).isEqualTo(METADATA);
        softly.assertThat(decodedDesign.getScript()).isEqualTo(SCRIPT);
        softly.assertAll();
    }

    public static void assertExpectedDesignDeleteRequestedEvent(DesignDeleteRequested actualEvent, UUID designId, UUID userId) {
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(actualEvent.getUserId()).isEqualTo(userId);
        softly.assertThat(actualEvent.getCommandId()).isNotNull();
        softly.assertThat(actualEvent.getDesignId()).isEqualTo(designId);
        softly.assertAll();
    }

    public static void assertExpectedDesignInsertCommandMessage(Row row, String uuid) {
        SoftAssertions softly = new SoftAssertions();
        final String actualType = row.getString("MESSAGE_TYPE");
        final String actualValue = row.getString("MESSAGE_VALUE");
        final UUID actualUuid = row.getUuid("MESSAGE_UUID");
        final String actualToken = row.getString("MESSAGE_TOKEN");
        final String actualSource = row.getString("MESSAGE_SOURCE");
        final String actualKey = row.getString("MESSAGE_KEY");
        final Instant actualTimestamp = row.getInstant("MESSAGE_TIMESTAMP");
        softly.assertThat(actualUuid).isNotNull();
        softly.assertThat(actualToken).isNotNull();
        softly.assertThat(actualValue).isNotNull();
        softly.assertThat(actualType).isEqualTo(DesignInsertCommand.TYPE);
        softly.assertThat(actualSource).isNotNull();
        softly.assertThat(actualKey).isEqualTo(uuid);
        softly.assertThat(actualTimestamp).isNotNull();
        softly.assertAll();
    }

    public static void assertExpectedDesignUpdateCommandMessage(Row row, String uuid) {
        SoftAssertions softly = new SoftAssertions();
        final String actualType = row.getString("MESSAGE_TYPE");
        final String actualValue = row.getString("MESSAGE_VALUE");
        final UUID actualUuid = row.getUuid("MESSAGE_UUID");
        final String actualToken = row.getString("MESSAGE_TOKEN");
        final String actualSource = row.getString("MESSAGE_SOURCE");
        final String actualKey = row.getString("MESSAGE_KEY");
        final Instant actualTimestamp = row.getInstant("MESSAGE_TIMESTAMP");
        softly.assertThat(actualUuid).isNotNull();
        softly.assertThat(actualToken).isNotNull();
        softly.assertThat(actualValue).isNotNull();
        softly.assertThat(actualType).isEqualTo(DesignUpdateCommand.TYPE);
        softly.assertThat(actualSource).isNotNull();
        softly.assertThat(actualKey).isEqualTo(uuid);
        softly.assertThat(actualTimestamp).isNotNull();
        softly.assertAll();
    }

    public static void assertExpectedDesignDeleteCommandMessage(Row row, String uuid) {
        SoftAssertions softly = new SoftAssertions();
        final String actualType = row.getString("MESSAGE_TYPE");
        final String actualValue = row.getString("MESSAGE_VALUE");
        final UUID actualUuid = row.getUuid("MESSAGE_UUID");
        final String actualToken = row.getString("MESSAGE_TOKEN");
        final String actualSource = row.getString("MESSAGE_SOURCE");
        final String actualKey = row.getString("MESSAGE_KEY");
        final Instant actualTimestamp = row.getInstant("MESSAGE_TIMESTAMP");
        softly.assertThat(actualUuid).isNotNull();
        softly.assertThat(actualToken).isNotNull();
        softly.assertThat(actualValue).isNotNull();
        softly.assertThat(actualType).isEqualTo(DesignDeleteCommand.TYPE);
        softly.assertThat(actualSource).isNotNull();
        softly.assertThat(actualKey).isEqualTo(uuid);
        softly.assertThat(actualTimestamp).isNotNull();
        softly.assertAll();
    }
}
