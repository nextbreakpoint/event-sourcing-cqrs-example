package com.nextbreakpoint.shop.designs;

import com.nextbreakpoint.shop.common.handlers.DefaultHandler;
import com.nextbreakpoint.shop.common.model.events.DeleteDesignEvent;
import com.nextbreakpoint.shop.common.model.events.DeleteDesignsEvent;
import com.nextbreakpoint.shop.common.model.events.InsertDesignEvent;
import com.nextbreakpoint.shop.common.model.Message;
import com.nextbreakpoint.shop.common.handlers.FailedMessageConsumer;
import com.nextbreakpoint.shop.common.handlers.MessageReceiptConsumer;
import com.nextbreakpoint.shop.common.model.events.UpdateDesignEvent;
import com.nextbreakpoint.shop.designs.controllers.delete.DeleteDesignController;
import com.nextbreakpoint.shop.designs.controllers.delete.DeleteDesignInputMapper;
import com.nextbreakpoint.shop.designs.controllers.delete.DeleteDesignsController;
import com.nextbreakpoint.shop.designs.controllers.delete.DeleteDesignsInputMapper;
import com.nextbreakpoint.shop.designs.controllers.insert.InsertDesignController;
import com.nextbreakpoint.shop.designs.controllers.insert.InsertDesignInputMapper;
import com.nextbreakpoint.shop.common.model.MessageReceipt;
import com.nextbreakpoint.shop.designs.controllers.update.UpdateDesignController;
import com.nextbreakpoint.shop.designs.controllers.update.UpdateDesignInputMapper;
import io.vertx.core.Handler;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;

public class Factory {
    private Factory() {}

    public static Handler<Message> createInsertDesignHandler(Store store, KafkaProducer<String, String> producer) {
        return DefaultHandler.<Message, InsertDesignEvent, MessageReceipt, MessageReceipt>builder()
                .withInputMapper(new InsertDesignInputMapper())
                .withOutputMapper(receipt -> receipt)
                .withController(new InsertDesignController(store, producer))
                .onSuccess(new MessageReceiptConsumer())
                .onFailure(new FailedMessageConsumer())
                .build();
    }

    public static Handler<Message> createUpdateDesignHandler(Store store, KafkaProducer<String, String> producer) {
        return DefaultHandler.<Message, UpdateDesignEvent, MessageReceipt, MessageReceipt>builder()
                .withInputMapper(new UpdateDesignInputMapper())
                .withOutputMapper(receipt -> receipt)
                .withController(new UpdateDesignController(store, producer))
                .onSuccess(new MessageReceiptConsumer())
                .onFailure(new FailedMessageConsumer())
                .build();
    }

    public static Handler<Message> createDeleteDesignHandler(Store store, KafkaProducer<String, String> producer) {
        return DefaultHandler.<Message, DeleteDesignEvent, MessageReceipt, MessageReceipt>builder()
                .withInputMapper(new DeleteDesignInputMapper())
                .withOutputMapper(receipt -> receipt)
                .withController(new DeleteDesignController(store, producer))
                .onSuccess(new MessageReceiptConsumer())
                .onFailure(new FailedMessageConsumer())
                .build();
    }

    public static Handler<Message> createDeleteDesignsHandler(Store store, KafkaProducer<String, String> producer) {
        return DefaultHandler.<Message, DeleteDesignsEvent, MessageReceipt, MessageReceipt>builder()
                .withInputMapper(new DeleteDesignsInputMapper())
                .withOutputMapper(receipt -> receipt)
                .withController(new DeleteDesignsController(store, producer))
                .onSuccess(new MessageReceiptConsumer())
                .onFailure(new FailedMessageConsumer())
                .build();
    }
}
