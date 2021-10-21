package com.nextbreakpoint.blueprint.designs.operations.update;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.designs.Store;
import com.nextbreakpoint.blueprint.designs.common.AbstractController;
import com.nextbreakpoint.blueprint.designs.model.DesignChanged;
import com.nextbreakpoint.blueprint.designs.model.PersistenceResult;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import rx.Single;

import java.util.UUID;

public class UpdateDesignController extends AbstractController<UpdateDesignCommand> {
    private final Logger logger = LoggerFactory.getLogger(UpdateDesignController.class.getName());

    public UpdateDesignController(Store store, String topic, KafkaProducer<String, String> producer, Mapper<DesignChanged, Message> mapper) {
        super(store, topic, producer, mapper, 3);
    }

    @Override
    protected Single<PersistenceResult<Void>> executeCommand(UpdateDesignCommand command, UUID eventTimestamp) {
        logger.info("Processing update command: " + command.getUuid() + ":" + eventTimestamp);
        return store.updateDesign(command.getUuid(), eventTimestamp, command.getJson());
    }
}
