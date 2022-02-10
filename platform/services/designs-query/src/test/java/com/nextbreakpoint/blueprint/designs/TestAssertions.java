package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentDeleteCompleted;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentDeleteRequested;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentUpdateCompleted;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentUpdateRequested;
import com.nextbreakpoint.blueprint.designs.model.Design;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class TestAssertions {
    private TestAssertions() {}

    public static void assertExpectedDesignDocumentUpdateRequestedMessage(UUID designId, InputMessage actualMessage, String data, String checksum, String status) {
        assertThat(actualMessage.getTimestamp()).isNotNull();
        assertThat(actualMessage.getOffset()).isNotNull();
        assertThat(actualMessage.getValue().getSource()).isEqualTo(TestConstants.MESSAGE_SOURCE);
        assertThat(actualMessage.getKey()).isEqualTo(designId.toString());
        assertThat(actualMessage.getValue().getUuid()).isNotNull();
        assertThat(actualMessage.getValue().getType()).isEqualTo(TestConstants.DESIGN_DOCUMENT_UPDATE_REQUESTED);
        DesignDocumentUpdateRequested actualEvent = Json.decodeValue(actualMessage.getValue().getData(), DesignDocumentUpdateRequested.class);
        assertThat(actualEvent.getDesignId()).isEqualTo(designId);
        assertThat(actualEvent.getRevision()).isNotNull();
        assertThat(actualEvent.getEventId()).isNotNull();
        assertThat(actualEvent.getData()).isEqualTo(data);
        assertThat(actualEvent.getStatus()).isEqualTo(status);
        assertThat(actualEvent.getChecksum()).isEqualTo(checksum);
        assertThat(actualEvent.getLevels()).isEqualTo(TestConstants.LEVELS);
        assertThat(actualEvent.getTiles()).isNotNull();
        assertThat(actualEvent.getModified()).isNotNull();
    }

    public static void assertExpectedDesignDocumentUpdateCompletedMessage(UUID designId, InputMessage actualMessage) {
        assertThat(actualMessage.getTimestamp()).isNotNull();
        assertThat(actualMessage.getOffset()).isNotNull();
        assertThat(actualMessage.getValue().getSource()).isEqualTo(TestConstants.MESSAGE_SOURCE);
        assertThat(actualMessage.getKey()).isEqualTo(designId.toString());
        assertThat(actualMessage.getValue().getUuid()).isNotNull();
        assertThat(actualMessage.getValue().getType()).isEqualTo(TestConstants.DESIGN_DOCUMENT_UPDATE_COMPLETED);
        DesignDocumentUpdateCompleted actualEvent = Json.decodeValue(actualMessage.getValue().getData(), DesignDocumentUpdateCompleted.class);
        assertThat(actualEvent.getDesignId()).isEqualTo(designId);
        assertThat(actualEvent.getRevision()).isNotNull();
        assertThat(actualEvent.getEventId()).isNotNull();
    }

    public static void assertExpectedDesignDocumentDeleteRequestedMessage(UUID designId, InputMessage actualMessage) {
        assertThat(actualMessage.getTimestamp()).isNotNull();
        assertThat(actualMessage.getOffset()).isNotNull();
        assertThat(actualMessage.getValue().getSource()).isEqualTo(TestConstants.MESSAGE_SOURCE);
        assertThat(actualMessage.getKey()).isEqualTo(designId.toString());
        assertThat(actualMessage.getValue().getUuid()).isNotNull();
        assertThat(actualMessage.getValue().getType()).isEqualTo(TestConstants.DESIGN_DOCUMENT_DELETE_REQUESTED);
        DesignDocumentDeleteRequested actualEvent = Json.decodeValue(actualMessage.getValue().getData(), DesignDocumentDeleteRequested.class);
        assertThat(actualEvent.getDesignId()).isEqualTo(designId);
        assertThat(actualEvent.getRevision()).isNotNull();
        assertThat(actualEvent.getEventId()).isNotNull();
    }

    public static void assertExpectedDesignDocumentDeleteCompletedMessage(UUID designId, InputMessage actualMessage) {
        assertThat(actualMessage.getTimestamp()).isNotNull();
        assertThat(actualMessage.getOffset()).isNotNull();
        assertThat(actualMessage.getValue().getSource()).isEqualTo(TestConstants.MESSAGE_SOURCE);
        assertThat(actualMessage.getKey()).isEqualTo(designId.toString());
        assertThat(actualMessage.getValue().getUuid()).isNotNull();
        assertThat(actualMessage.getValue().getType()).isEqualTo(TestConstants.DESIGN_DOCUMENT_DELETE_COMPLETED);
        DesignDocumentDeleteCompleted actualEvent = Json.decodeValue(actualMessage.getValue().getData(), DesignDocumentDeleteCompleted.class);
        assertThat(actualEvent.getDesignId()).isEqualTo(designId);
        assertThat(actualEvent.getRevision()).isNotNull();
        assertThat(actualEvent.getEventId()).isNotNull();
    }

    public static void assertExpectedDesign(Design design, UUID designId, String data, String checksum, String status) {
        assertThat(design.getDesignId()).isEqualTo(designId);
        assertThat(design.getEventId()).isNotNull();
        assertThat(design.getRevision()).isNotNull();
        assertThat(design.getEventId()).isNotNull();
        assertThat(design.getData()).isEqualTo(data);
        assertThat(design.getStatus()).isEqualTo(status);
        assertThat(design.getChecksum()).isEqualTo(checksum);
        assertThat(design.getLevels()).isEqualTo(TestConstants.LEVELS);
        assertThat(design.getTiles()).isNotNull();
        assertThat(design.getTiles().get(0).getLevel()).isEqualTo(0);
        assertThat(design.getTiles().get(0).getRequested()).isEqualTo(1);
        assertThat(design.getTiles().get(0).getCompleted()).hasSize(1);
        assertThat(design.getTiles().get(0).getFailed()).isEmpty();
        assertThat(design.getTiles().get(1).getLevel()).isEqualTo(1);
        assertThat(design.getTiles().get(1).getRequested()).isEqualTo(4);
        assertThat(design.getTiles().get(1).getCompleted()).hasSize(4);
        assertThat(design.getTiles().get(1).getFailed()).isEmpty();
        assertThat(design.getTiles().get(2).getLevel()).isEqualTo(2);
        assertThat(design.getTiles().get(2).getRequested()).isEqualTo(16);
        assertThat(design.getTiles().get(2).getCompleted()).hasSize(16);
        assertThat(design.getTiles().get(2).getFailed()).isEmpty();
        assertThat(design.getModified()).isNotNull();
    }
}
