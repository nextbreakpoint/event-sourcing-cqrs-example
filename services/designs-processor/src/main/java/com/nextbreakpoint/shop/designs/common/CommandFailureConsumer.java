package com.nextbreakpoint.shop.designs.common;

import com.nextbreakpoint.shop.designs.model.CommandRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.kafka.client.common.TopicPartition;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumer;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public class CommandFailureConsumer implements BiConsumer<CommandRequest, Throwable> {
    private final Logger logger = LoggerFactory.getLogger(CommandFailureConsumer.class.getName());

    private final KafkaConsumer<String, String> consumer;

    public CommandFailureConsumer(KafkaConsumer<String, String> consumer) {
        this.consumer = Objects.requireNonNull(consumer);
    }

    @Override
    public void accept(CommandRequest request, Throwable error) {
        final TopicPartition topicPartition = new TopicPartition(request.getRecord().topic(), request.getRecord().partition());

        consumer.rxPause(topicPartition)
                .flatMap(x -> consumer.rxSeek(topicPartition, request.getRecord().offset()))
                .delay(5, TimeUnit.SECONDS)
                .flatMap(x -> consumer.rxResume(topicPartition))
                .subscribe();

        logger.error("Failed to process message: id=" + request.getMessage().getMessageId(), error);
    }
}
