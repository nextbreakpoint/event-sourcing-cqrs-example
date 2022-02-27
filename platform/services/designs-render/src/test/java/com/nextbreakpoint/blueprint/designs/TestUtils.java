package com.nextbreakpoint.blueprint.designs;

import au.com.dius.pact.core.model.V4Interaction;
import au.com.dius.pact.core.model.messaging.Message;
import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.KafkaRecord;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.core.Tracing;
import com.nextbreakpoint.blueprint.common.events.TileRenderRequested;
import com.nextbreakpoint.blueprint.common.test.PayloadUtils;
import org.jetbrains.annotations.NotNull;

public class TestUtils {
    private TestUtils() {}

    @NotNull
    public static String createBucketKey(TileRenderRequested event) {
        return String.format("%s/%d/%04d%04d.png", event.getChecksum(), event.getLevel(), event.getRow(), event.getCol());
    }

    @NotNull
    public static OutputMessage toOutputMessage(V4Interaction.AsynchronousMessage message) {
        final KafkaRecord kafkaRecord = Json.decodeValue(message.getContents().getContents().valueAsString(), KafkaRecord.class);
        return OutputMessage.from(kafkaRecord.getKey(), PayloadUtils.mapToPayload(kafkaRecord.getValue()), Tracing.from(kafkaRecord.getHeaders()));
    }
}
