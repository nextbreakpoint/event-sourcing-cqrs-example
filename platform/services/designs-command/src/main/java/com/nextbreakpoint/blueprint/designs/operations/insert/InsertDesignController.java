package com.nextbreakpoint.blueprint.designs.operations.insert;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.DesignInsertRequested;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.common.vertx.KafkaEmitter;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import rx.Single;

import java.util.Objects;
import java.util.UUID;

public class InsertDesignController implements Controller<InsertDesignRequest, InsertDesignResponse> {
    private static final Logger logger = LoggerFactory.getLogger(InsertDesignController.class.getName());

    private final Mapper<InsertDesignRequest, DesignInsertRequested> inputMapper;
    private final MessageMapper<DesignInsertRequested, OutputMessage> outputMapper;
    private final KafkaEmitter emitter;

    public InsertDesignController(Mapper<InsertDesignRequest, DesignInsertRequested> inputMapper, MessageMapper<DesignInsertRequested, OutputMessage> outputMapper, KafkaEmitter emitter) {
        this.emitter = Objects.requireNonNull(emitter);
        this.inputMapper = Objects.requireNonNull(inputMapper);
        this.outputMapper = Objects.requireNonNull(outputMapper);
    }

    @Override
    public Single<InsertDesignResponse> onNext(InsertDesignRequest request) {
        return Single.just(request)
                .map(this.inputMapper::transform)
                .doOnSuccess(event -> logger.info("Processing event " + event))
                .map(event -> outputMapper.transform(Tracing.of(null), event))
                .flatMap(emitter::onNext)
                .map(ignore -> new InsertDesignResponse(request.getUuid(), ResultStatus.SUCCESS))
                .onErrorReturn(err -> new InsertDesignResponse(request.getUuid(), ResultStatus.FAILURE, err.getMessage()));
    }
}
