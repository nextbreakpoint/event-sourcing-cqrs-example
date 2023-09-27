package com.nextbreakpoint.blueprint.designs;

import au.com.dius.pact.core.model.V4Interaction;
import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.KafkaRecord;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.test.PayloadUtils;
import org.jetbrains.annotations.NotNull;

public class TestUtils {
    private TestUtils() {}

    @NotNull
    public static OutputMessage toOutputMessage(V4Interaction.AsynchronousMessage message) {
        final KafkaRecord kafkaRecord = Json.decodeValue(message.getContents().getContents().valueAsString(), KafkaRecord.class);
        return OutputMessage.from(kafkaRecord.getKey(), PayloadUtils.mapToPayload(kafkaRecord.getValue()));
    }
}
