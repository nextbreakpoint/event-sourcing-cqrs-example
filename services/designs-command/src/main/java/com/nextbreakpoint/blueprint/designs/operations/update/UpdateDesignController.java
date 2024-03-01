package com.nextbreakpoint.blueprint.designs.operations.update;

import com.nextbreakpoint.blueprint.common.commands.avro.DesignUpdateCommand;
import com.nextbreakpoint.blueprint.common.core.Controller;
import com.nextbreakpoint.blueprint.common.core.MessageEmitter;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.core.ResultStatus;
import com.nextbreakpoint.blueprint.common.vertx.MessageFactory;
import com.nextbreakpoint.blueprint.designs.common.DesignsRenderClient;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;
import rx.Single;

import java.util.Objects;

@Log4j2
public class UpdateDesignController implements Controller<UpdateDesignRequest, UpdateDesignResponse> {
    private final String messageSource;
    private final MessageEmitter<DesignUpdateCommand> emitter;
    private final DesignsRenderClient designsRenderClient;

    public UpdateDesignController(String messageSource, MessageEmitter<DesignUpdateCommand> emitter, DesignsRenderClient designsRenderClient) {
        this.messageSource = Objects.requireNonNull(messageSource);
        this.emitter = Objects.requireNonNull(emitter);
        this.designsRenderClient = Objects.requireNonNull(designsRenderClient);
    }

    @Override
    public Single<UpdateDesignResponse> onNext(UpdateDesignRequest request) {
        return Single.just(request)
                .flatMap(this::validateDesign)
                .map(this::createCommand)
                .doOnSuccess(command -> log.info("Processing update command {}", command.getDesignId()))
                .map(this::createMessage)
                .flatMap(emitter::send)
                .map(ignore -> createResponse(request))
                .doOnError(err -> log.error("Can't process update command", err))
                .onErrorReturn(err -> createResponse(request, err));
    }

    private Single<UpdateDesignRequest> validateDesign(UpdateDesignRequest request) {
        return designsRenderClient.validateDesign(request.getToken(), new JsonObject(request.getJson()))
                .map(response -> response.getString("status"))
                .flatMap(status -> handleResponse(request, status));
    }

    private Single<UpdateDesignRequest> handleResponse(UpdateDesignRequest request, String status) {
        return status.equals("ACCEPTED") ? Single.just(request) : Single.error(new RuntimeException("Design was rejected"));
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
