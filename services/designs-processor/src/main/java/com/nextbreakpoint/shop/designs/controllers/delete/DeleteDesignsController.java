package com.nextbreakpoint.shop.designs.controllers.delete;

import com.nextbreakpoint.shop.common.model.Controller;
import com.nextbreakpoint.shop.common.model.events.DeleteDesignsEvent;
import com.nextbreakpoint.shop.designs.Store;
import com.nextbreakpoint.shop.common.model.MessageReceipt;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import rx.Single;

import java.util.Objects;

public class DeleteDesignsController implements Controller<DeleteDesignsEvent, MessageReceipt> {
    private final Store store;
    private final KafkaProducer<String, String> producer;

    public DeleteDesignsController(Store store, KafkaProducer<String, String> producer) {
        this.store = Objects.requireNonNull(store);
        this.producer = Objects.requireNonNull(producer);
    }

    @Override
    public Single<MessageReceipt> onNext(DeleteDesignsEvent event) {
        return null;
    }
}
