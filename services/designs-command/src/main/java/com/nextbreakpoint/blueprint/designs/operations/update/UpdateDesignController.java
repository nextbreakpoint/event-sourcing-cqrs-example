package com.nextbreakpoint.blueprint.designs.operations.update;

import com.nextbreakpoint.blueprint.common.commands.avro.DesignUpdateCommand;
import com.nextbreakpoint.blueprint.common.core.Controller;
import com.nextbreakpoint.blueprint.common.core.MessageEmitter;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.core.ResultStatus;
import com.nextbreakpoint.blueprint.common.vertx.MessageFactory;
import lombok.extern.log4j.Log4j2;
import rx.Single;

import java.util.Objects;

@Log4j2
public class UpdateDesignController implements Controller<UpdateDesignRequest, UpdateDesignResponse> {
    private final String messageSource;
    private final MessageEmitter<DesignUpdateCommand> emitter;

    public UpdateDesignController(String messageSource, MessageEmitter<DesignUpdateCommand> emitter) {
        this.messageSource = Objects.requireNonNull(messageSource);
        this.emitter = Objects.requireNonNull(emitter);
    }

    @Override
    public Single<UpdateDesignResponse> onNext(UpdateDesignRequest request) {
        return Single.just(request)
                .map(this::createCommand)
                .doOnSuccess(command -> log.info("Processing update command {}", command.getDesignId()))
                .map(this::createMessage)
                .flatMap(emitter::send)
                .map(ignore -> createResponse(request))
                .doOnError(err -> log.error("Can't process update command", err))
                .onErrorReturn(err -> createResponse(request, err));
    }

    private DesignUpdateCommand createCommand(UpdateDesignRequest request) {
        return DesignUpdateCommand.newBuilder()
                .setUserId(request.getOwner())
                .setDesignId(request.getUuid())
                .setCommandId(request.getChange())
                .setData(request.getJson())
                .setPublished(request.getPublished())
                .build();
    }

    private OutputMessage<DesignUpdateCommand> createMessage(DesignUpdateCommand command) {
        return MessageFactory.<DesignUpdateCommand>of(messageSource)
                .createOutputMessage(command.getDesignId().toString(), command);
    }

    private static UpdateDesignResponse createResponse(UpdateDesignRequest request) {
        return UpdateDesignResponse.builder()
                .withUuid(request.getUuid())
                .withStatus(ResultStatus.SUCCESS)
                .build();
    }

    private static UpdateDesignResponse createResponse(UpdateDesignRequest request, Throwable throwable) {
        return UpdateDesignResponse.builder()
                .withUuid(request.getUuid())
                .withStatus(ResultStatus.FAILURE)
                .withError(throwable.getMessage())
                .build();
    }
}
