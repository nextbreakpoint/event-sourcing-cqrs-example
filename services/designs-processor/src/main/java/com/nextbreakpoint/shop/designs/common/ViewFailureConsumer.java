package com.nextbreakpoint.shop.designs.common;

import com.nextbreakpoint.shop.designs.model.RecordAndMessage;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.kafka.client.common.TopicPartition;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumer;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public class ViewFailureConsumer implements BiConsumer<RecordAndMessage, Throwable> {
    private final Logger logger = LoggerFactory.getLogger(ViewFailureConsumer.class.getName());

    private final KafkaConsumer<String, String> consumer;

    public ViewFailureConsumer(KafkaConsumer<String, String> consumer) {
        this.consumer = Objects.requireNonNull(consumer);
    }

    @Override
    public void accept(RecordAndMessage input, Throwable error) {
        final TopicPartition topicPartition = new TopicPartition(input.getRecord().topic(), input.getRecord().partition());

        consumer.rxPause(topicPartition)
                .flatMap(x -> consumer.rxSeek(topicPartition, input.getRecord().offset()))
                .delay(5, TimeUnit.SECONDS)
                .flatMap(x -> consumer.rxResume(topicPartition))
                .subscribe();

        logger.error("Failed to process message: id=" + input.getMessage().getMessageId(), error);
    }
}
