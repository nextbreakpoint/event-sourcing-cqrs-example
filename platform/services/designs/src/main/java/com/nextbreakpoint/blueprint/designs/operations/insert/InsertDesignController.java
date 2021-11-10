package com.nextbreakpoint.blueprint.designs.operations.insert;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.designs.Store;
import com.nextbreakpoint.blueprint.designs.common.AbstractController;
import com.nextbreakpoint.blueprint.designs.model.DesignChanged;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.Json;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import rx.Single;

public class InsertDesignController extends AbstractController<InsertDesignRequest, InsertDesignResponse> {
    private Logger logger = LoggerFactory.getLogger(InsertDesignController.class);

    public InsertDesignController(Store store, String topic, KafkaProducer<String, String> producer, Mapper<DesignChanged, OutputMessage> mapper) {
        super(store ,topic, producer, mapper);
    }

    @Override
    protected Single<InsertDesignResponse> execute(InsertDesignRequest request) {
        return store.insertDesign(request);
    }

    @Override
    protected String createKey(InsertDesignResponse response) {
        return response.getUuid().toString();
    }

    @Override
    protected String createValue(InsertDesignResponse response) {
        return Json.encode(mapper.transform(new DesignChanged(response.getUuid(), System.currentTimeMillis())));
    }
}
