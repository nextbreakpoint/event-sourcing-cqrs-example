package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.events.DesignDeleteRequested;
import com.nextbreakpoint.blueprint.common.events.DesignInsertRequested;
import com.nextbreakpoint.blueprint.common.events.DesignUpdateRequested;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class TestAssertions {
    private TestAssertions() {}

    public static void assertExpectedDesignInsertRequestedMessage(InputMessage actualMessage, String designId) {
        assertThat(actualMessage.getTimestamp()).isNotNull();
        assertThat(actualMessage.getOffset()).isNotNull();
        assertThat(actualMessage.getKey()).isEqualTo(designId);
        assertThat(actualMessage.getValue()).isNotNull();
        assertThat(actualMessage.getValue().getSource()).isEqualTo(TestConstants.MESSAGE_SOURCE);
        assertThat(actualMessage.getValue().getUuid()).isNotNull();
        assertThat(actualMessage.getValue().getType()).isEqualTo(TestConstants.DESIGN_INSERT_REQUESTED);
        assertThat(actualMessage.getValue()).isNotNull();
        assertThat(actualMessage.getTrace().getTraceId()).isNotNull();
        assertThat(actualMessage.getTrace().getSpanId()).isNotNull();
        assertThat(actualMessage.getTrace().getParent()).isNull();
        DesignInsertRequested actualEvent = Json.decodeValue(actualMessage.getValue().getData(), DesignInsertRequested.class);
        assertThat(actualEvent.getEventId()).isNotNull();
        assertThat(actualEvent.getUserId()).isEqualTo(TestConstants.USER_ID);
        assertThat(actualEvent.getChangeId()).isNotNull();
        assertThat(actualEvent.getDesignId()).isEqualTo(UUID.fromString(designId));
        assertThat(actualEvent.getLevels()).isEqualTo(8);
        assertThat(actualEvent.getData()).isNotNull();
        Design decodedDesign = Json.decodeValue(actualEvent.getData(), Design.class);
        assertThat(decodedDesign.getManifest()).isEqualTo(TestConstants.MANIFEST);
        assertThat(decodedDesign.getMetadata()).isEqualTo(TestConstants.METADATA);
        assertThat(decodedDesign.getScript()).isEqualTo(TestConstants.SCRIPT);
    }

    public static void assertExpectedDesignUpdateRequestedMessage(InputMessage actualMessage, String designId) {
        assertThat(actualMessage.getTimestamp()).isNotNull();
        assertThat(actualMessage.getOffset()).isNotNull();
        assertThat(actualMessage.getKey()).isEqualTo(designId);
        assertThat(actualMessage.getValue()).isNotNull();
        assertThat(actualMessage.getValue().getSource()).isEqualTo(TestConstants.MESSAGE_SOURCE);
        assertThat(actualMessage.getValue().getUuid()).isNotNull();
        assertThat(actualMessage.getValue().getType()).isEqualTo(TestConstants.DESIGN_UPDATE_REQUESTED);
        assertThat(actualMessage.getValue()).isNotNull();
        assertThat(actualMessage.getTrace().getTraceId()).isNotNull();
        assertThat(actualMessage.getTrace().getSpanId()).isNotNull();
        assertThat(actualMessage.getTrace().getParent()).isNull();
        DesignUpdateRequested actualEvent = Json.decodeValue(actualMessage.getValue().getData(), DesignUpdateRequested.class);
        assertThat(actualEvent.getEventId()).isNotNull();
        assertThat(actualEvent.getUserId()).isEqualTo(TestConstants.USER_ID);
        assertThat(actualEvent.getChangeId()).isNotNull();
        assertThat(actualEvent.getDesignId()).isEqualTo(UUID.fromString(designId));
        assertThat(actualEvent.getLevels()).isEqualTo(8);
        assertThat(actualEvent.getData()).isNotNull();
        Design decodedDesign = Json.decodeValue(actualEvent.getData(), Design.class);
        assertThat(decodedDesign.getManifest()).isEqualTo(TestConstants.MANIFEST);
        assertThat(decodedDesign.getMetadata()).isEqualTo(TestConstants.METADATA);
        assertThat(decodedDesign.getScript()).isEqualTo(TestConstants.SCRIPT);
    }

    public static void assertExpectedDesignDeleteRequestedMessage(InputMessage actualMessage, String designId) {
        assertThat(actualMessage.getTimestamp()).isNotNull();
        assertThat(actualMessage.getOffset()).isNotNull();
        assertThat(actualMessage.getKey()).isEqualTo(designId);
        assertThat(actualMessage.getValue()).isNotNull();
        assertThat(actualMessage.getValue().getSource()).isEqualTo(TestConstants.MESSAGE_SOURCE);
        assertThat(actualMessage.getValue().getUuid()).isNotNull();
        assertThat(actualMessage.getValue().getType()).isEqualTo(TestConstants.DESIGN_DELETE_REQUESTED);
        assertThat(actualMessage.getValue()).isNotNull();
        assertThat(actualMessage.getTrace().getTraceId()).isNotNull();
        assertThat(actualMessage.getTrace().getSpanId()).isNotNull();
        assertThat(actualMessage.getTrace().getParent()).isNull();
        DesignDeleteRequested actualEvent = Json.decodeValue(actualMessage.getValue().getData(), DesignDeleteRequested.class);
        assertThat(actualEvent.getEventId()).isNotNull();
        assertThat(actualEvent.getUserId()).isEqualTo(TestConstants.USER_ID);
        assertThat(actualEvent.getChangeId()).isNotNull();
        assertThat(actualEvent.getDesignId()).isEqualTo(UUID.fromString(designId));
    }
}
