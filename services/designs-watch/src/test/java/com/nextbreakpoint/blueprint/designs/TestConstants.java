package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.events.avro.DesignDocumentDeleteCompleted;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDocumentUpdateCompleted;

import java.util.UUID;

public interface TestConstants {
    String DESIGN_DOCUMENT_UPDATE_COMPLETED = DesignDocumentUpdateCompleted.getClassSchema().getFullName();
    String DESIGN_DOCUMENT_DELETE_COMPLETED = DesignDocumentDeleteCompleted.getClassSchema().getFullName();

    String MESSAGE_SOURCE = "service-designs";
    String EVENTS_TOPIC_NAME = "test-designs-watch-events";

    String UUID1_REGEXP = "[0-9a-f]{8}-[0-9a-f]{4}-[1][0-9a-f]{3}-[0-9a-f]{4}-[0-9a-f]{12}";
    String UUID6_REGEXP = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

    String REVISION_REGEXP = "[0-9]{16}-[0-9]{16}";

    String REVISION_0 = "0000000000000000-0000000000000000";
    String REVISION_1 = "0000000000000000-0000000000000001";
    String REVISION_2 = "0000000000000000-0000000000000002";

    UUID DESIGN_ID_1 = new UUID(0L, 1L);
    UUID DESIGN_ID_2 = new UUID(0L, 2L);

    UUID COMMAND_ID_1 = new UUID(1L, 1L);
    UUID COMMAND_ID_2 = new UUID(1L, 2L);
}
