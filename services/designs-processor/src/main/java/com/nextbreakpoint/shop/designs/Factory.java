package com.nextbreakpoint.shop.designs;

import com.nextbreakpoint.shop.common.MessageHandler;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;

public class Factory {
    private Factory() {}

    public static MessageHandler createInsertDesignHandler(Store store, KafkaProducer<String, String> producer) {
        return null;
    }

    public static MessageHandler createUpdateDesignHandler(Store store, KafkaProducer<String, String> producer) {
        return null;
    }

    public static MessageHandler createDeleteDesignHandler(Store store, KafkaProducer<String, String> producer) {
        return null;
    }

    public static MessageHandler createDeleteDesignsHandler(Store store, KafkaProducer<String, String> producer) {
        return null;
    }
}
