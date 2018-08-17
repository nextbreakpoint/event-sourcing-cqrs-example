package com.nextbreakpoint.shop.designs.handlers;

import com.nextbreakpoint.shop.common.Controller;
import com.nextbreakpoint.shop.common.DeleteDesignEvent;
import com.nextbreakpoint.shop.designs.Store;
import com.nextbreakpoint.shop.common.MessageReceipt;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import rx.Single;

import java.util.Objects;

public class DeleteDesignController implements Controller<DeleteDesignEvent, MessageReceipt> {
    private final Store store;
    private final KafkaProducer<String, String> producer;

    public DeleteDesignController(Store store, KafkaProducer<String, String> producer) {
        this.store = Objects.requireNonNull(store);
        this.producer = Objects.requireNonNull(producer);
    }

    @Override
    public Single<MessageReceipt> onNext(DeleteDesignEvent event) {
        return null;
    }
}
