package com.nextbreakpoint.shop.designs.delete;

import com.nextbreakpoint.shop.common.Controller;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import rx.Single;

import java.util.Objects;

public class DeleteDesignController implements Controller<DeleteDesignRequest, DeleteDesignResponse> {
    private final KafkaProducer<String, String> producer;

    public DeleteDesignController(KafkaProducer<String, String> producer) {
        this.producer = Objects.requireNonNull(producer);
    }

    @Override
    public Single<DeleteDesignResponse> apply(DeleteDesignRequest request) {
        return Single.error(new RuntimeException());
    }
}
