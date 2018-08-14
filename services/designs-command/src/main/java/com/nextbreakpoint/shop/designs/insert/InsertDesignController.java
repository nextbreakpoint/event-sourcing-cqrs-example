package com.nextbreakpoint.shop.designs.insert;

import com.nextbreakpoint.shop.common.Controller;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import rx.Single;

import java.util.Objects;

public class InsertDesignController implements Controller<InsertDesignRequest, InsertDesignResponse> {
    private final KafkaProducer<String, String> producer;

    public InsertDesignController(KafkaProducer<String, String> producer) {
        this.producer = Objects.requireNonNull(producer);
    }

    @Override
    public Single<InsertDesignResponse> apply(InsertDesignRequest request) {
        return Single.error(new RuntimeException());
    }
}
