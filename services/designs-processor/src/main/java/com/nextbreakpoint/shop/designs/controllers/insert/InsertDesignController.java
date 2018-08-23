package com.nextbreakpoint.shop.designs.controllers.insert;

import com.nextbreakpoint.shop.common.model.Controller;
import com.nextbreakpoint.shop.common.model.events.InsertDesignEvent;
import com.nextbreakpoint.shop.designs.Store;
import com.nextbreakpoint.shop.common.model.MessageReceipt;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import rx.Single;

import java.util.Objects;

public class InsertDesignController implements Controller<InsertDesignEvent, MessageReceipt> {
    private final Store store;
    private final KafkaProducer<String, String> producer;

    public InsertDesignController(Store store, KafkaProducer<String, String> producer) {
        this.store = Objects.requireNonNull(store);
        this.producer = Objects.requireNonNull(producer);
    }

    @Override
    public Single<MessageReceipt> onNext(InsertDesignEvent event) {
        return store.insertDesign(event)
                .map(result -> new MessageReceipt());
    }
}
