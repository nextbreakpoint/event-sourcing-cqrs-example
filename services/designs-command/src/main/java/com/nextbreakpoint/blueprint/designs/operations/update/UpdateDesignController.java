package com.nextbreakpoint.blueprint.designs.operations.update;

import com.nextbreakpoint.blueprint.common.commands.DesignUpdateCommand;
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
public class UpdateDesignController implements Controller<UpdateDesignRequest, UpdateDesignResponse> {
    private final Mapper<UpdateDesignRequest, DesignUpdateCommand> inputMapper;
    private final MessageMapper<DesignUpdateCommand, OutputMessage> outputMapper;
    private final MessageEmitter emitter;

    public UpdateDesignController(Mapper<UpdateDesignRequest, DesignUpdateCommand> inputMapper, MessageMapper<DesignUpdateCommand, OutputMessage> outputMapper, MessageEmitter emitter) {
        this.emitter = Objects.requireNonNull(emitter);
        this.inputMapper = Objects.requireNonNull(inputMapper);
        this.outputMapper = Objects.requireNonNull(outputMapper);
    }

    @Override
    public Single<UpdateDesignResponse> onNext(UpdateDesignRequest request) {
        return Single.just(request)
                .map(this.inputMapper::transform)
                .doOnSuccess(command -> log.info("Processing update command {}", command.getDesignId()))
                .map(outputMapper::transform)
                .flatMap(emitter::send)
                .map(ignore -> UpdateDesignResponse.builder().withUuid(request.getUuid()).withStatus(ResultStatus.SUCCESS).build())
                .doOnError(err -> log.info("Can't process update command", err))
                .onErrorReturn(err -> UpdateDesignResponse.builder().withUuid(request.getUuid()).withStatus(ResultStatus.FAILURE).withError(err.getMessage()).build());
    }
}
