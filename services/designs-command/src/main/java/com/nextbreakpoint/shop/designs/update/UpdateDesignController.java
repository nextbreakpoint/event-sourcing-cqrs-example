package com.nextbreakpoint.shop.designs.update;

import com.nextbreakpoint.shop.common.Controller;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import rx.Single;

import java.util.Objects;

public class UpdateDesignController implements Controller<UpdateDesignRequest, UpdateDesignResponse> {
    private final KafkaProducer<String, String> producer;

    public UpdateDesignController(KafkaProducer<String, String> producer) {
        this.producer = Objects.requireNonNull(producer);
    }

    @Override
    public Single<UpdateDesignResponse> apply(UpdateDesignRequest request) {
        return Single.error(new RuntimeException());
    }
}
