package com.nextbreakpoint.blueprint.designs.operations.insert;

import com.nextbreakpoint.blueprint.common.commands.DesignInsertCommand;
import com.nextbreakpoint.blueprint.common.core.*;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import rx.Single;

import java.util.Objects;

public class InsertDesignController implements Controller<InsertDesignRequest, InsertDesignResponse> {
    private static final Logger logger = LoggerFactory.getLogger(InsertDesignController.class.getName());

    private final Mapper<InsertDesignRequest, DesignInsertCommand> inputMapper;
    private final MessageMapper<DesignInsertCommand, OutputMessage> outputMapper;
    private final MessageEmitter emitter;

    public InsertDesignController(Mapper<InsertDesignRequest, DesignInsertCommand> inputMapper, MessageMapper<DesignInsertCommand, OutputMessage> outputMapper, MessageEmitter emitter) {
        this.emitter = Objects.requireNonNull(emitter);
        this.inputMapper = Objects.requireNonNull(inputMapper);
        this.outputMapper = Objects.requireNonNull(outputMapper);
    }

    @Override
    public Single<InsertDesignResponse> onNext(InsertDesignRequest request) {
        return Single.just(request)
                .map(this.inputMapper::transform)
                .doOnSuccess(command -> logger.info("Processing insert command " + command.getDesignId()))
                .map(outputMapper::transform)
                .flatMap(emitter::send)
                .map(ignore -> new InsertDesignResponse(request.getUuid(), ResultStatus.SUCCESS))
                .doOnError(err -> logger.info("Can't process insert command", err))
                .onErrorReturn(err -> new InsertDesignResponse(request.getUuid(), ResultStatus.FAILURE, err.getMessage()));
    }
}
