package com.nextbreakpoint.blueprint.designs;

import au.com.dius.pact.core.model.messaging.Message;
import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.KafkaRecord;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.core.Tracing;
import com.nextbreakpoint.blueprint.common.test.PayloadUtils;
import org.jetbrains.annotations.NotNull;

public class TestUtils {
    private TestUtils() {}

    @NotNull
    public static OutputMessage toOutputMessage(Message message) {
        final KafkaRecord kafkaRecord = Json.decodeValue(message.contentsAsString(), KafkaRecord.class);
        return OutputMessage.from(kafkaRecord.getKey(), PayloadUtils.mapToPayload(kafkaRecord.getValue()), Tracing.from(kafkaRecord.getHeaders()));
    }
}
