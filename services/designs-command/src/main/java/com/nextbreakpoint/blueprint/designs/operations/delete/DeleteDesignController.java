package com.nextbreakpoint.blueprint.designs.operations.delete;

import com.nextbreakpoint.blueprint.common.commands.avro.DesignDeleteCommand;
import com.nextbreakpoint.blueprint.common.core.Controller;
import com.nextbreakpoint.blueprint.common.core.MessageEmitter;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.core.ResultStatus;
import com.nextbreakpoint.blueprint.common.vertx.MessageFactory;
import lombok.extern.log4j.Log4j2;
import rx.Single;

import java.util.Objects;

@Log4j2
public class DeleteDesignController implements Controller<DeleteDesignRequest, DeleteDesignResponse> {
    private final String messageSource;
    private final MessageEmitter<DesignDeleteCommand> emitter;

    public DeleteDesignController(String messageSource, MessageEmitter<DesignDeleteCommand> emitter) {
        this.messageSource = Objects.requireNonNull(messageSource);
        this.emitter = Objects.requireNonNull(emitter);
    }

    @Override
    public Single<DeleteDesignResponse> onNext(DeleteDesignRequest request) {
        return Single.just(request)
                .map(this::createCommand)
                .doOnSuccess(command -> log.info("Processing delete command {}", command.getDesignId()))
                .map(this::createMessage)
                .flatMap(emitter::send)
                .map(ignore -> createResponse(request))
                .doOnError(err -> log.error("Can't process delete command", err))
                .onErrorReturn(err -> createResponse(request, err));
    }

    private DesignDeleteCommand createCommand(DeleteDesignRequest request) {
        return DesignDeleteCommand.newBuilder()
                .setUserId(request.getOwner())
                .setDesignId(request.getUuid())
                .setCommandId(request.getChange())
                .build();
    }

    private OutputMessage<DesignDeleteCommand> createMessage(DesignDeleteCommand command) {
        return MessageFactory.<DesignDeleteCommand>of(messageSource)
                .createOutputMessage(command.getDesignId().toString(), command);
    }

    private static DeleteDesignResponse createResponse(DeleteDesignRequest request) {
        return DeleteDesignResponse.builder()
                .withUuid(request.getUuid())
                .withStatus(ResultStatus.SUCCESS)
                .build();
    }

    private static DeleteDesignResponse createResponse(DeleteDesignRequest request, Throwable throwable) {
        return DeleteDesignResponse.builder()
                .withUuid(request.getUuid())
                .withStatus(ResultStatus.FAILURE)
                .withError(throwable.getMessage())
                .build();
    }
}
