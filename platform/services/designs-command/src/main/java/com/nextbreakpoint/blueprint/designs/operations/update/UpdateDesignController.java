package com.nextbreakpoint.blueprint.designs.operations.update;

import com.nextbreakpoint.blueprint.common.commands.DesignUpdateCommand;
import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.common.vertx.KafkaEmitter;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import rx.Single;

import java.util.Objects;

public class UpdateDesignController implements Controller<UpdateDesignRequest, UpdateDesignResponse> {
    private static final Logger logger = LoggerFactory.getLogger(UpdateDesignController.class.getName());

    private final Mapper<UpdateDesignRequest, DesignUpdateCommand> inputMapper;
    private final MessageMapper<DesignUpdateCommand, OutputMessage> outputMapper;
    private final KafkaEmitter emitter;

    public UpdateDesignController(Mapper<UpdateDesignRequest, DesignUpdateCommand> inputMapper, MessageMapper<DesignUpdateCommand, OutputMessage> outputMapper, KafkaEmitter emitter) {
        this.emitter = Objects.requireNonNull(emitter);
        this.inputMapper = Objects.requireNonNull(inputMapper);
        this.outputMapper = Objects.requireNonNull(outputMapper);
    }

    @Override
    public Single<UpdateDesignResponse> onNext(UpdateDesignRequest request) {
        return Single.just(request)
                .map(this.inputMapper::transform)
                .doOnSuccess(command -> logger.info("Processing update command " + command.getDesignId()))
                .map(command -> outputMapper.transform(Tracing.of(null), command))
                .flatMap(emitter::send)
                .map(ignore -> new UpdateDesignResponse(request.getUuid(), ResultStatus.SUCCESS))
                .onErrorReturn(err -> new UpdateDesignResponse(request.getUuid(), ResultStatus.FAILURE, err.getMessage()));
    }
}
