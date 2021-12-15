package com.nextbreakpoint.blueprint.designs.operations.update;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.core.ResultStatus;
import com.nextbreakpoint.blueprint.common.events.DesignUpdateRequested;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.common.vertx.KafkaEmitter;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import rx.Single;

import java.util.Objects;

public class UpdateDesignController implements Controller<UpdateDesignRequest, UpdateDesignResponse> {
    private static final Logger logger = LoggerFactory.getLogger(UpdateDesignController.class.getName());

    private final Mapper<UpdateDesignRequest, DesignUpdateRequested> inputMapper;
    private final Mapper<DesignUpdateRequested, OutputMessage> outputMapper;
    private final KafkaEmitter emitter;

    public UpdateDesignController(Mapper<UpdateDesignRequest, DesignUpdateRequested> inputMapper, Mapper<DesignUpdateRequested, OutputMessage> outputMapper, KafkaEmitter emitter) {
        this.emitter = Objects.requireNonNull(emitter);
        this.inputMapper = Objects.requireNonNull(inputMapper);
        this.outputMapper = Objects.requireNonNull(outputMapper);
    }

    @Override
    public Single<UpdateDesignResponse> onNext(UpdateDesignRequest request) {
        return Single.just(request)
                .map(this.inputMapper::transform)
                .doOnSuccess(event -> logger.info("Processing event " + event))
                .map(this.outputMapper::transform)
                .flatMap(emitter::onNext)
                .map(ignore -> new UpdateDesignResponse(request.getUuid(), ResultStatus.SUCCESS))
                .onErrorReturn(err -> new UpdateDesignResponse(request.getUuid(), ResultStatus.FAILURE, err.getMessage()));
    }
}
