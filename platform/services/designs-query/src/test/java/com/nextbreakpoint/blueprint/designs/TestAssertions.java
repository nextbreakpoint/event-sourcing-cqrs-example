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

    public static void assertExpectedDesignDocumentUpdateRequestedMessage(InputMessage actualMessage, UUID designId, String data, String checksum, String status) {
        assertThat(actualMessage.getTimestamp()).isNotNull();
        assertThat(actualMessage.getKey()).isEqualTo(designId.toString());
        assertThat(actualMessage.getToken()).isNotNull();
        assertThat(actualMessage.getValue()).isNotNull();
        assertThat(actualMessage.getValue().getSource()).isEqualTo(TestConstants.MESSAGE_SOURCE);
        assertThat(actualMessage.getValue().getUuid()).isNotNull();
        assertThat(actualMessage.getValue().getType()).isEqualTo(TestConstants.DESIGN_DOCUMENT_UPDATE_REQUESTED);
        assertThat(actualMessage.getTrace()).isNotNull();
        assertThat(actualMessage.getTrace().getTraceId()).isNotNull();
        assertThat(actualMessage.getTrace().getSpanId()).isNotNull();
        assertThat(actualMessage.getTrace().getParent()).isNotNull();
        DesignDocumentUpdateRequested actualEvent = Json.decodeValue(actualMessage.getValue().getData(), DesignDocumentUpdateRequested.class);
        assertThat(actualEvent.getDesignId()).isEqualTo(designId);
        assertThat(actualEvent.getRevision()).isNotNull();
        assertThat(actualEvent.getData()).isEqualTo(data);
        assertThat(actualEvent.getStatus()).isEqualTo(status);
        assertThat(actualEvent.getChecksum()).isEqualTo(checksum);
        assertThat(actualEvent.getLevels()).isEqualTo(TestConstants.LEVELS);
        assertThat(actualEvent.getTiles()).isNotNull();
        assertThat(actualEvent.getModified()).isNotNull();
    }

    public static void assertExpectedDesignDocumentUpdateCompletedMessage(InputMessage actualMessage, UUID designId) {
        assertThat(actualMessage.getTimestamp()).isNotNull();
        assertThat(actualMessage.getKey()).isEqualTo(designId.toString());
        assertThat(actualMessage.getToken()).isNotNull();
        assertThat(actualMessage.getValue()).isNotNull();
        assertThat(actualMessage.getValue().getSource()).isEqualTo(TestConstants.MESSAGE_SOURCE);
        assertThat(actualMessage.getValue().getUuid()).isNotNull();
        assertThat(actualMessage.getValue().getType()).isEqualTo(TestConstants.DESIGN_DOCUMENT_UPDATE_COMPLETED);
        assertThat(actualMessage.getTrace()).isNotNull();
        assertThat(actualMessage.getTrace().getTraceId()).isNotNull();
        assertThat(actualMessage.getTrace().getSpanId()).isNotNull();
        assertThat(actualMessage.getTrace().getParent()).isNotNull();
        DesignDocumentUpdateCompleted actualEvent = Json.decodeValue(actualMessage.getValue().getData(), DesignDocumentUpdateCompleted.class);
        assertThat(actualEvent.getDesignId()).isEqualTo(designId);
        assertThat(actualEvent.getRevision()).isNotNull();
    }

    public static void assertExpectedDesignDocumentDeleteRequestedMessage(InputMessage actualMessage, UUID designId) {
        assertThat(actualMessage.getTimestamp()).isNotNull();
        assertThat(actualMessage.getKey()).isEqualTo(designId.toString());
        assertThat(actualMessage.getToken()).isNotNull();
        assertThat(actualMessage.getValue()).isNotNull();
        assertThat(actualMessage.getValue().getSource()).isEqualTo(TestConstants.MESSAGE_SOURCE);
        assertThat(actualMessage.getValue().getUuid()).isNotNull();
        assertThat(actualMessage.getValue().getType()).isEqualTo(TestConstants.DESIGN_DOCUMENT_DELETE_REQUESTED);
        assertThat(actualMessage.getTrace()).isNotNull();
        assertThat(actualMessage.getTrace().getTraceId()).isNotNull();
        assertThat(actualMessage.getTrace().getSpanId()).isNotNull();
        assertThat(actualMessage.getTrace().getParent()).isNotNull();
        DesignDocumentDeleteRequested actualEvent = Json.decodeValue(actualMessage.getValue().getData(), DesignDocumentDeleteRequested.class);
        assertThat(actualEvent.getDesignId()).isEqualTo(designId);
        assertThat(actualEvent.getRevision()).isNotNull();
    }

    public static void assertExpectedDesignDocumentDeleteCompletedMessage(InputMessage actualMessage, UUID designId) {
        assertThat(actualMessage.getTimestamp()).isNotNull();
        assertThat(actualMessage.getKey()).isEqualTo(designId.toString());
        assertThat(actualMessage.getToken()).isNotNull();
        assertThat(actualMessage.getValue()).isNotNull();
        assertThat(actualMessage.getValue().getSource()).isEqualTo(TestConstants.MESSAGE_SOURCE);
        assertThat(actualMessage.getValue().getUuid()).isNotNull();
        assertThat(actualMessage.getValue().getType()).isEqualTo(TestConstants.DESIGN_DOCUMENT_DELETE_COMPLETED);
        assertThat(actualMessage.getTrace()).isNotNull();
        assertThat(actualMessage.getTrace().getTraceId()).isNotNull();
        assertThat(actualMessage.getTrace().getSpanId()).isNotNull();
        assertThat(actualMessage.getTrace().getParent()).isNotNull();
        DesignDocumentDeleteCompleted actualEvent = Json.decodeValue(actualMessage.getValue().getData(), DesignDocumentDeleteCompleted.class);
        assertThat(actualEvent.getDesignId()).isEqualTo(designId);
        assertThat(actualEvent.getRevision()).isNotNull();
    }

    public static void assertExpectedDesign(Design actualDesign, UUID designId, String data, String checksum, String status) {
        assertThat(actualDesign.getDesignId()).isEqualTo(designId);
        assertThat(actualDesign.getCommandId()).isNotNull();
        assertThat(actualDesign.getUserId()).isNotNull();
        assertThat(actualDesign.getRevision()).isNotNull();
        assertThat(actualDesign.getData()).isEqualTo(data);
        assertThat(actualDesign.getStatus()).isEqualTo(status);
        assertThat(actualDesign.getChecksum()).isEqualTo(checksum);
        assertThat(actualDesign.getLevels()).isEqualTo(TestConstants.LEVELS);
        assertThat(actualDesign.getTiles()).isNotNull();
        assertThat(actualDesign.getTiles().get(0).getLevel()).isEqualTo(0);
        assertThat(actualDesign.getTiles().get(0).getRequested()).isEqualTo(1);
        assertThat(actualDesign.getTiles().get(0).getCompleted()).isEqualTo(1);
        assertThat(actualDesign.getTiles().get(0).getFailed()).isEqualTo(0);
        assertThat(actualDesign.getTiles().get(1).getLevel()).isEqualTo(1);
        assertThat(actualDesign.getTiles().get(1).getRequested()).isEqualTo(4);
        assertThat(actualDesign.getTiles().get(1).getCompleted()).isEqualTo(4);
        assertThat(actualDesign.getTiles().get(1).getFailed()).isEqualTo(0);
        assertThat(actualDesign.getTiles().get(2).getLevel()).isEqualTo(2);
        assertThat(actualDesign.getTiles().get(2).getRequested()).isEqualTo(16);
        assertThat(actualDesign.getTiles().get(2).getCompleted()).isEqualTo(16);
        assertThat(actualDesign.getTiles().get(2).getFailed()).isEqualTo(0);
        assertThat(actualDesign.getLastModified()).isNotNull();
    }
}
