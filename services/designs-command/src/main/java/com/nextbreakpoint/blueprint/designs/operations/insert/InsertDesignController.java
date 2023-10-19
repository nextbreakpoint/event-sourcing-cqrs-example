package com.nextbreakpoint.blueprint.designs.operations.insert;

import com.nextbreakpoint.blueprint.common.commands.DesignInsertCommand;
import com.nextbreakpoint.blueprint.common.core.*;
import lombok.extern.log4j.Log4j2;
import rx.Single;

import java.util.Objects;

@Log4j2
public class InsertDesignController implements Controller<InsertDesignRequest, InsertDesignResponse> {
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
                .doOnSuccess(command -> log.info("Processing insert command {}", command.getDesignId()))
                .map(outputMapper::transform)
                .flatMap(emitter::send)
                .map(ignore -> InsertDesignResponse.builder().withUuid(request.getUuid()).withStatus(ResultStatus.SUCCESS).build())
                .doOnError(err -> log.info("Can't process insert command", err))
                .onErrorReturn(err -> InsertDesignResponse.builder().withUuid(request.getUuid()).withStatus(ResultStatus.FAILURE).withError(err.getMessage()).build());

    }
}
