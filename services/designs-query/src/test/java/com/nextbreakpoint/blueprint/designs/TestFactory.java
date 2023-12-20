package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDocumentDeleteCompleted;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDocumentDeleteRequested;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDocumentUpdateCompleted;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDocumentUpdateRequested;
import com.nextbreakpoint.blueprint.common.test.MessageUtils;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_DOCUMENT_DELETE_COMPLETED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_DOCUMENT_DELETE_REQUESTED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_DOCUMENT_UPDATE_COMPLETED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_DOCUMENT_UPDATE_REQUESTED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.MESSAGE_SOURCE;

public class TestFactory {
    private TestFactory() {}

    @NotNull
    public static InputMessage<DesignDocumentUpdateRequested> createInputMessage(UUID messageId, String messageToken, LocalDateTime messageTime, DesignDocumentUpdateRequested designDocumentUpdateRequested) {
        return MessageUtils.createInputMessage(
                MESSAGE_SOURCE, designDocumentUpdateRequested.getDesignId().toString(), DESIGN_DOCUMENT_UPDATE_REQUESTED, messageId, designDocumentUpdateRequested, messageToken, messageTime
        );
    }

    @NotNull
    public static InputMessage<DesignDocumentDeleteRequested> createInputMessage(UUID messageId, String messageToken, LocalDateTime messageTime, DesignDocumentDeleteRequested designDocumentDeleteRequested) {
        return MessageUtils.createInputMessage(
                MESSAGE_SOURCE, designDocumentDeleteRequested.getDesignId().toString(), DESIGN_DOCUMENT_DELETE_REQUESTED, messageId, designDocumentDeleteRequested, messageToken, messageTime
        );
    }

    @NotNull
    public static OutputMessage<DesignDocumentUpdateCompleted> createOutputMessage(UUID messageId, DesignDocumentUpdateCompleted designDocumentUpdateCompleted) {
        return MessageUtils.createOutputMessage(
                MESSAGE_SOURCE, designDocumentUpdateCompleted.getDesignId().toString(), DESIGN_DOCUMENT_UPDATE_COMPLETED, messageId, designDocumentUpdateCompleted
        );
    }

    @NotNull
    public static OutputMessage<DesignDocumentDeleteCompleted> createOutputMessage(UUID messageId, DesignDocumentDeleteCompleted designDocumentDeleteCompleted) {
        return MessageUtils.createOutputMessage(
                MESSAGE_SOURCE, designDocumentDeleteCompleted.getDesignId().toString(), DESIGN_DOCUMENT_DELETE_COMPLETED, messageId, designDocumentDeleteCompleted
        );
    }
}
