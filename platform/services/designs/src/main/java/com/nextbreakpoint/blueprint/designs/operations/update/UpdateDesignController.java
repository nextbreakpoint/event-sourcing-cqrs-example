package com.nextbreakpoint.blueprint.designs.operations.update;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.designs.Store;
import com.nextbreakpoint.blueprint.designs.common.AbstractController;
import com.nextbreakpoint.blueprint.designs.model.DesignChanged;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.Json;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import rx.Single;

public class UpdateDesignController extends AbstractController<UpdateDesignRequest, UpdateDesignResponse> {
    private Logger logger = LoggerFactory.getLogger(UpdateDesignController.class);

    public UpdateDesignController(Store store, String topic, KafkaProducer<String, String> producer, Mapper<DesignChanged, Message> mapper) {
        super(store ,topic, producer ,mapper);
    }

    @Override
    protected Single<UpdateDesignResponse> execute(UpdateDesignRequest request) {
        return store.updateDesign(request);
    }

    @Override
    protected String createKey(UpdateDesignResponse response) {
        return response.getUuid().toString();
    }

    @Override
    protected String createValue(UpdateDesignResponse response) {
        return Json.encode(mapper.transform(new DesignChanged(response.getUuid(), System.currentTimeMillis())));
    }
}
