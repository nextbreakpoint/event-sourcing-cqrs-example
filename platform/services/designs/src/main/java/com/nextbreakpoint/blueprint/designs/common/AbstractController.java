package com.nextbreakpoint.blueprint.designs.common;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.designs.Store;
import com.nextbreakpoint.blueprint.designs.model.DesignChanged;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import io.vertx.rxjava.kafka.client.producer.KafkaProducerRecord;
import rx.Single;

import java.util.Objects;

public abstract class AbstractController<T, R> implements Controller<T, R> {
    private Logger logger = LoggerFactory.getLogger(AbstractController.class);

    protected final Store store;
    protected final String topic;
    protected final KafkaProducer<String, String> producer;
    protected final Mapper<DesignChanged, Message> mapper;

    public AbstractController(Store store, String topic, KafkaProducer<String, String> producer, Mapper<DesignChanged, Message> mapper) {
        this.store = Objects.requireNonNull(store);
        this.topic = Objects.requireNonNull(topic);
        this.producer = Objects.requireNonNull(producer);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public Single<R> onNext(T request) {
        return execute(request).flatMap(this::publishRecordOrFailQuietly);
    }

    private Single<R> publishRecordOrFailQuietly(R response) {
        return createRecord(response)
                .flatMap(record -> producer.rxWrite(record).map(metadata -> response))
                .doOnError(err -> logger.error("Can't send message. The operation will be retried later", err))
                .onErrorReturn(err -> response);
    }

    private Single<KafkaProducerRecord<String, String>> createRecord(R response) {
        return Single.fromCallable(() -> KafkaProducerRecord.create(topic, createKey(response), createValue(response)));
    }

    protected abstract Single<R> execute(T request);

    protected abstract String createKey(R response);

    protected abstract String createValue(R response);
}
