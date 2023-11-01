package com.nextbreakpoint.blueprint.designs.operations.delete;

import com.nextbreakpoint.blueprint.common.commands.DesignDeleteCommand;
import com.nextbreakpoint.blueprint.common.core.Controller;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.MessageEmitter;
import com.nextbreakpoint.blueprint.common.core.MessageMapper;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.core.ResultStatus;
import lombok.extern.log4j.Log4j2;
import rx.Single;

import java.util.Objects;

@Log4j2
public class DeleteDesignController implements Controller<DeleteDesignRequest, DeleteDesignResponse> {
    private final Mapper<DeleteDesignRequest, DesignDeleteCommand> inputMapper;
    private final MessageMapper<DesignDeleteCommand, OutputMessage> outputMapper;
    private final MessageEmitter emitter;

    public DeleteDesignController(Mapper<DeleteDesignRequest, DesignDeleteCommand> inputMapper, MessageMapper<DesignDeleteCommand, OutputMessage> outputMapper, MessageEmitter emitter) {
        this.emitter = Objects.requireNonNull(emitter);
        this.inputMapper = Objects.requireNonNull(inputMapper);
        this.outputMapper = Objects.requireNonNull(outputMapper);
    }

    @Override
    public Single<DeleteDesignResponse> onNext(DeleteDesignRequest request) {
        return Single.just(request)
                .map(this.inputMapper::transform)
                .doOnSuccess(command -> log.info("Processing delete command {}", command.getDesignId()))
                .map(outputMapper::transform)
                .flatMap(emitter::send)
                .map(ignore -> DeleteDesignResponse.builder().withUuid(request.getUuid()).withStatus(ResultStatus.SUCCESS).build())
                .doOnError(err -> log.info("Can't process delete command", err))
                .onErrorReturn(err -> DeleteDesignResponse.builder().withUuid(request.getUuid()).withStatus(ResultStatus.FAILURE).withError(err.getMessage()).build());
    }
}
