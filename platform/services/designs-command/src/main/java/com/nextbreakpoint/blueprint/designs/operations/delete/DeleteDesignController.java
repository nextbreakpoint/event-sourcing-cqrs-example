package com.nextbreakpoint.blueprint.designs.operations.delete;

import com.nextbreakpoint.blueprint.common.commands.DesignDeleteCommand;
import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.common.vertx.KafkaEmitter;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import rx.Single;

import java.util.Objects;

public class DeleteDesignController implements Controller<DeleteDesignRequest, DeleteDesignResponse> {
    private static final Logger logger = LoggerFactory.getLogger(DeleteDesignController.class.getName());

    private final Mapper<DeleteDesignRequest, DesignDeleteCommand> inputMapper;
    private final MessageMapper<DesignDeleteCommand, OutputMessage> outputMapper;
    private final KafkaEmitter emitter;

    public DeleteDesignController(Mapper<DeleteDesignRequest, DesignDeleteCommand> inputMapper, MessageMapper<DesignDeleteCommand, OutputMessage> outputMapper, KafkaEmitter emitter) {
        this.emitter = Objects.requireNonNull(emitter);
        this.inputMapper = Objects.requireNonNull(inputMapper);
        this.outputMapper = Objects.requireNonNull(outputMapper);
    }

    @Override
    public Single<DeleteDesignResponse> onNext(DeleteDesignRequest request) {
        return Single.just(request)
                .map(this.inputMapper::transform)
                .doOnSuccess(command -> logger.info("Processing delete command " + command.getDesignId()))
                .map(command -> outputMapper.transform(Tracing.of(null), command))
                .flatMap(emitter::onNext)
                .map(ignore -> new DeleteDesignResponse(request.getUuid(), ResultStatus.SUCCESS))
                .onErrorReturn(err -> new DeleteDesignResponse(request.getUuid(), ResultStatus.FAILURE, err.getMessage()));
    }
}
