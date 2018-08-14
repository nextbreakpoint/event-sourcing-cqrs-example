package com.nextbreakpoint.shop.designs.delete;

import com.nextbreakpoint.shop.common.Controller;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import rx.Single;

import java.util.Objects;

public class DeleteDesignsController implements Controller<DeleteDesignsRequest, DeleteDesignsResponse> {
    private final KafkaProducer<String, String> producer;

    public DeleteDesignsController(KafkaProducer<String, String> producer) {
        this.producer = Objects.requireNonNull(producer);
    }

    @Override
    public Single<DeleteDesignsResponse> apply(DeleteDesignsRequest request) {
        return Single.error(new RuntimeException());
    }
}
