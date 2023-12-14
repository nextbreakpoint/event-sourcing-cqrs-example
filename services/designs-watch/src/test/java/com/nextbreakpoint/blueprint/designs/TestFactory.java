package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDocumentDeleteCompleted;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDocumentUpdateCompleted;
import com.nextbreakpoint.blueprint.common.test.MessageUtils;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_DOCUMENT_DELETE_COMPLETED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_DOCUMENT_UPDATE_COMPLETED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.MESSAGE_SOURCE;

public class TestFactory {
    private TestFactory() {}

    @NotNull
    public static InputMessage<DesignDocumentUpdateCompleted> createInputMessage(UUID messageId, String messageToken, LocalDateTime messageTime, DesignDocumentUpdateCompleted designDocumentUpdateCompleted) {
        return MessageUtils.createInputMessage(
                MESSAGE_SOURCE, designDocumentUpdateCompleted.getDesignId().toString(), DESIGN_DOCUMENT_UPDATE_COMPLETED, messageId, designDocumentUpdateCompleted, messageToken, messageTime
        );
    }

    @NotNull
    public static InputMessage<DesignDocumentDeleteCompleted> createInputMessage(UUID messageId, String messageToken, LocalDateTime messageTime, DesignDocumentDeleteCompleted designDocumentDeleteCompleted) {
        return MessageUtils.createInputMessage(
                MESSAGE_SOURCE, designDocumentDeleteCompleted.getDesignId().toString(), DESIGN_DOCUMENT_DELETE_COMPLETED, messageId, designDocumentDeleteCompleted, messageToken, messageTime
        );
    }
}
