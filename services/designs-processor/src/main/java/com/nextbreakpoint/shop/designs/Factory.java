package com.nextbreakpoint.shop.designs;

import com.nextbreakpoint.shop.common.DefaultHandler;
import com.nextbreakpoint.shop.common.DeleteDesignEvent;
import com.nextbreakpoint.shop.common.DeleteDesignsEvent;
import com.nextbreakpoint.shop.common.InsertDesignEvent;
import com.nextbreakpoint.shop.common.Message;
import com.nextbreakpoint.shop.common.MessageFailedHandler;
import com.nextbreakpoint.shop.common.MessageSentHandler;
import com.nextbreakpoint.shop.common.UpdateDesignEvent;
import com.nextbreakpoint.shop.designs.handlers.DeleteDesignController;
import com.nextbreakpoint.shop.designs.handlers.DeleteDesignInputMapper;
import com.nextbreakpoint.shop.designs.handlers.DeleteDesignsController;
import com.nextbreakpoint.shop.designs.handlers.DeleteDesignsInputMapper;
import com.nextbreakpoint.shop.designs.handlers.InsertDesignController;
import com.nextbreakpoint.shop.designs.handlers.InsertDesignInputMapper;
import com.nextbreakpoint.shop.common.MessageReceipt;
import com.nextbreakpoint.shop.designs.handlers.UpdateDesignController;
import com.nextbreakpoint.shop.designs.handlers.UpdateDesignInputMapper;
import io.vertx.core.Handler;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;

public class Factory {
    private Factory() {}

    public static Handler<Message> createInsertDesignHandler(Store store, KafkaProducer<String, String> producer) {
        return DefaultHandler.<Message, InsertDesignEvent, MessageReceipt, MessageReceipt>builder()
                .withInputMapper(new InsertDesignInputMapper())
                .withOutputMapper(receipt -> receipt)
                .withController(new InsertDesignController(store, producer))
                .onSuccess(new MessageSentHandler())
                .onFailure(new MessageFailedHandler())
                .build();
    }

    public static Handler<Message> createUpdateDesignHandler(Store store, KafkaProducer<String, String> producer) {
        return DefaultHandler.<Message, UpdateDesignEvent, MessageReceipt, MessageReceipt>builder()
                .withInputMapper(new UpdateDesignInputMapper())
                .withOutputMapper(receipt -> receipt)
                .withController(new UpdateDesignController(store, producer))
                .onSuccess(new MessageSentHandler())
                .onFailure(new MessageFailedHandler())
                .build();
    }

    public static Handler<Message> createDeleteDesignHandler(Store store, KafkaProducer<String, String> producer) {
        return DefaultHandler.<Message, DeleteDesignEvent, MessageReceipt, MessageReceipt>builder()
                .withInputMapper(new DeleteDesignInputMapper())
                .withOutputMapper(receipt -> receipt)
                .withController(new DeleteDesignController(store, producer))
                .onSuccess(new MessageSentHandler())
                .onFailure(new MessageFailedHandler())
                .build();
    }

    public static Handler<Message> createDeleteDesignsHandler(Store store, KafkaProducer<String, String> producer) {
        return DefaultHandler.<Message, DeleteDesignsEvent, MessageReceipt, MessageReceipt>builder()
                .withInputMapper(new DeleteDesignsInputMapper())
                .withOutputMapper(receipt -> receipt)
                .withController(new DeleteDesignsController(store, producer))
                .onSuccess(new MessageSentHandler())
                .onFailure(new MessageFailedHandler())
                .build();
    }
}
