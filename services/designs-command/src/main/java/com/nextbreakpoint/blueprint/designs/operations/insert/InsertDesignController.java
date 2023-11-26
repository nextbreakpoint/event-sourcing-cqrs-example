package com.nextbreakpoint.blueprint.designs.operations.insert;

import com.nextbreakpoint.blueprint.common.commands.avro.DesignInsertCommand;
import com.nextbreakpoint.blueprint.common.core.Controller;
import com.nextbreakpoint.blueprint.common.core.MessageEmitter;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.core.ResultStatus;
import com.nextbreakpoint.blueprint.common.vertx.MessageFactory;
import lombok.extern.log4j.Log4j2;
import rx.Single;

import java.util.Objects;

@Log4j2
public class InsertDesignController implements Controller<InsertDesignRequest, InsertDesignResponse> {
    private final String messageSource;
    private final MessageEmitter<DesignInsertCommand> emitter;

    public InsertDesignController(String messageSource, MessageEmitter<DesignInsertCommand> emitter) {
        this.messageSource = Objects.requireNonNull(messageSource);
        this.emitter = Objects.requireNonNull(emitter);
    }

    @Override
    public Single<InsertDesignResponse> onNext(InsertDesignRequest request) {
        return Single.just(request)
                .map(this::createCommand)
                .doOnSuccess(command -> log.info("Processing insert command {}", command.getDesignId()))
                .map(this::createMessage)
                .flatMap(emitter::send)
                .map(ignore -> createResponse(request))
                .doOnError(err -> log.error("Can't process insert command", err))
                .onErrorReturn(err -> createResponse(request, err));

    }

    private DesignInsertCommand createCommand(InsertDesignRequest request) {
        return DesignInsertCommand.newBuilder()
                .setUserId(request.getOwner())
                .setDesignId(request.getUuid())
                .setCommandId(request.getChange())
                .setData(request.getJson())
                .build();
    }

    private OutputMessage<DesignInsertCommand> createMessage(DesignInsertCommand command) {
        return MessageFactory.<DesignInsertCommand>of(messageSource)
                .createOutputMessage(command.getDesignId().toString(), command);
    }

    private static InsertDesignResponse createResponse(InsertDesignRequest request) {
        return InsertDesignResponse.builder()
                .withUuid(request.getUuid())
                .withStatus(ResultStatus.SUCCESS)
                .build();
    }

    private static InsertDesignResponse createResponse(InsertDesignRequest request, Throwable throwable) {
        return InsertDesignResponse.builder()
                .withUuid(request.getUuid())
                .withStatus(ResultStatus.FAILURE)
                .withError(throwable.getMessage())
                .build();
    }
}
