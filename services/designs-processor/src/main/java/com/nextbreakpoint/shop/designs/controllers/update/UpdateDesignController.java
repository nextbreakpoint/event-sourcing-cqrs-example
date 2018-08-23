package com.nextbreakpoint.shop.designs.controllers.update;

import com.nextbreakpoint.shop.common.model.Controller;
import com.nextbreakpoint.shop.common.model.events.UpdateDesignEvent;
import com.nextbreakpoint.shop.designs.Store;
import com.nextbreakpoint.shop.common.model.MessageReceipt;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import rx.Single;

import java.util.Objects;

public class UpdateDesignController implements Controller<UpdateDesignEvent, MessageReceipt> {
    private final Store store;
    private final KafkaProducer<String, String> producer;

    public UpdateDesignController(Store store, KafkaProducer<String, String> producer) {
        this.store = Objects.requireNonNull(store);
        this.producer = Objects.requireNonNull(producer);
    }

    @Override
    public Single<MessageReceipt> onNext(UpdateDesignEvent event) {
        return store.updateDesign(event)
                .map(result -> new MessageReceipt());
    }
}
