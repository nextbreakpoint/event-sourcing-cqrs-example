package com.nextbreakpoint.shop.designs.common;

import com.nextbreakpoint.shop.common.model.events.DesignChangedEvent;
import com.nextbreakpoint.shop.designs.model.RecordAndMessage;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumer;

import java.util.Objects;
import java.util.function.BiConsumer;

public class ViewSuccessConsumer implements BiConsumer<RecordAndMessage, DesignChangedEvent> {
    private final Logger logger = LoggerFactory.getLogger(ViewSuccessConsumer.class.getName());

    private final KafkaConsumer<String, String> consumer;

    public ViewSuccessConsumer(KafkaConsumer<String, String> consumer) {
        this.consumer = Objects.requireNonNull(consumer);
    }

    @Override
    public void accept(RecordAndMessage input, DesignChangedEvent event) {
        consumer.commit();
        logger.info("Design changed: id=" + event.getUuid());
    }
}
