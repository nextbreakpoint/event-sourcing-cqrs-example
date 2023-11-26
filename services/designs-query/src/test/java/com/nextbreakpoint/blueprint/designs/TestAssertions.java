package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDocumentDeleteCompleted;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDocumentUpdateCompleted;
import com.nextbreakpoint.blueprint.designs.model.Design;
import com.nextbreakpoint.blueprint.designs.model.LevelTiles;
import org.assertj.core.api.SoftAssertions;

import java.util.List;
import java.util.UUID;

public class TestAssertions {
    private TestAssertions() {}

    public static void assertExpectedDesignDocumentUpdateCompletedMessage(InputMessage<Object> actualMessage, UUID designId) {
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(actualMessage.getTimestamp()).isNotNull();
        softly.assertThat(actualMessage.getKey()).isEqualTo(designId.toString());
        softly.assertThat(actualMessage.getToken()).isNotNull();
        softly.assertThat(actualMessage.getValue()).isNotNull();
        softly.assertThat(actualMessage.getValue().getSource()).isEqualTo(TestConstants.MESSAGE_SOURCE);
        softly.assertThat(actualMessage.getValue().getUuid()).isNotNull();
        softly.assertThat(actualMessage.getValue().getType()).isEqualTo(TestConstants.DESIGN_DOCUMENT_UPDATE_COMPLETED);
        softly.assertAll();
    }

    public static void assertExpectedDesignDocumentDeleteCompletedMessage(InputMessage<Object> actualMessage, UUID designId) {
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(actualMessage.getTimestamp()).isNotNull();
        softly.assertThat(actualMessage.getKey()).isEqualTo(designId.toString());
        softly.assertThat(actualMessage.getToken()).isNotNull();
        softly.assertThat(actualMessage.getValue()).isNotNull();
        softly.assertThat(actualMessage.getValue().getSource()).isEqualTo(TestConstants.MESSAGE_SOURCE);
        softly.assertThat(actualMessage.getValue().getUuid()).isNotNull();
        softly.assertThat(actualMessage.getValue().getType()).isEqualTo(TestConstants.DESIGN_DOCUMENT_DELETE_COMPLETED);
        softly.assertAll();
    }

    public static void assertExpectedDesignDocumentUpdateCompletedEvent(DesignDocumentUpdateCompleted actualEvent, UUID designId) {
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(actualEvent.getDesignId()).isEqualTo(designId);
        softly.assertThat(actualEvent.getCommandId()).isNotNull();
        softly.assertThat(actualEvent.getRevision()).isNotNull();
        softly.assertAll();
    }

    public static void assertExpectedDesignDocumentDeleteCompletedEvent(DesignDocumentDeleteCompleted actualEvent, UUID designId) {
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(actualEvent.getDesignId()).isEqualTo(designId);
        softly.assertThat(actualEvent.getCommandId()).isNotNull();
        softly.assertThat(actualEvent.getRevision()).isNotNull();
        softly.assertAll();
    }

    public static void assertExpectedDesign(Design actualDesign, UUID designId, UUID commandId, UUID userId, String data, String checksum, String revision, String status, List<LevelTiles> tiles, int levels) {
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(actualDesign.getDesignId()).isEqualTo(designId);
        softly.assertThat(actualDesign.getCommandId()).isEqualTo(commandId);
        softly.assertThat(actualDesign.getUserId()).isEqualTo(userId);
        softly.assertThat(actualDesign.getRevision()).isEqualTo(revision);
        softly.assertThat(actualDesign.getData()).isEqualTo(data);
        softly.assertThat(actualDesign.getStatus()).isEqualTo(status);
        softly.assertThat(actualDesign.getChecksum()).isEqualTo(checksum);
        softly.assertThat(actualDesign.getLevels()).isEqualTo(levels);
        softly.assertThat(actualDesign.getTiles()).isEqualTo(tiles);
        softly.assertThat(actualDesign.getUpdated()).isNotNull();
        softly.assertAll();
    }
}
