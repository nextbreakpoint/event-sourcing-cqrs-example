package com.nextbreakpoint.blueprint.designs.common;

import com.nextbreakpoint.blueprint.common.core.event.DesignChanged;
import com.nextbreakpoint.blueprint.designs.model.RecordAndMessage;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumer;

import java.util.Objects;
import java.util.function.BiConsumer;

public class ViewSuccessConsumer implements BiConsumer<RecordAndMessage, DesignChanged> {
    private final Logger logger = LoggerFactory.getLogger(ViewSuccessConsumer.class.getName());

    private final KafkaConsumer<String, String> consumer;

    public ViewSuccessConsumer(KafkaConsumer<String, String> consumer) {
        this.consumer = Objects.requireNonNull(consumer);
    }

    @Override
    public void accept(RecordAndMessage input, DesignChanged event) {
        consumer.commit();
        logger.info("Designs view changed: id=" + event.getUuid());
    }
}
