package com.nextbreakpoint.blueprint.designs;

import au.com.dius.pact.core.model.V4Interaction;
import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.test.KafkaRecord;
import com.nextbreakpoint.blueprint.common.test.PayloadUtils;
import org.apache.avro.specific.SpecificRecord;
import org.jetbrains.annotations.NotNull;

public class TestUtils {
    private TestUtils() {}

    @NotNull
    public static <T extends SpecificRecord> OutputMessage<T> toOutputMessage(V4Interaction.AsynchronousMessage message, Class<T> clazz) {
        final String json = message.getContents().getContents().valueAsString();
        final KafkaRecord kafkaRecord = Json.decodeValue(json, KafkaRecord.class);

        return OutputMessage.<T>builder()
                .withKey(kafkaRecord.getKey())
                .withValue(PayloadUtils.mapToPayload(kafkaRecord.getValue(), clazz))
                .build();
    }
}
