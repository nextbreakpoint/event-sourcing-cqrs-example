package com.nextbreakpoint.blueprint.designs.operations.delete;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.designs.Store;
import com.nextbreakpoint.blueprint.designs.common.AbstractController;
import com.nextbreakpoint.blueprint.designs.model.DesignChanged;
import com.nextbreakpoint.blueprint.designs.model.PersistenceResult;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import rx.Single;

import java.util.UUID;

public class DeleteDesignController extends AbstractController<DeleteDesignCommand> {
    private final Logger logger = LoggerFactory.getLogger(DeleteDesignController.class.getName());

    public DeleteDesignController(Store store, String topic, KafkaProducer<String, String> producer, Mapper<DesignChanged, OutputMessage> mapper) {
        super(store, topic, producer, mapper, 3);
    }

    @Override
    protected Single<PersistenceResult<Void>> executeCommand(DeleteDesignCommand command, UUID eventTimestamp) {
        logger.info("Processing delete command: " + command.getUuid() + ":" + eventTimestamp);
        return store.deleteDesign(command.getUuid(), eventTimestamp);
    }
}
