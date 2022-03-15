package com.nextbreakpoint.blueprint.designs.operations.delete;

import com.nextbreakpoint.blueprint.common.commands.DesignDeleteCommand;
import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.common.vertx.MessageEmitter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import rx.Single;

import java.util.Objects;

public class DeleteDesignController implements Controller<DeleteDesignRequest, DeleteDesignResponse> {
    private static final Logger logger = LoggerFactory.getLogger(DeleteDesignController.class.getName());

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
        return Single.just(Context.current())
                .map(context -> {
                    final Span span = Span.current();
                    return Tracing.of(span.getSpanContext().getTraceId(), span.getSpanContext().getSpanId());
                })
                .flatMap(tracing -> Single.just(request)
                        .map(this.inputMapper::transform)
                        .doOnSuccess(command -> logger.info("Processing delete command " + command.getDesignId()))
                        .map(event -> outputMapper.transform(event, tracing))
                        .flatMap(emitter::send)
                        .map(ignore -> new DeleteDesignResponse(request.getUuid(), ResultStatus.SUCCESS))
                        .doOnError(err -> logger.info("Can't process delete command", err))
                        .onErrorReturn(err -> new DeleteDesignResponse(request.getUuid(), ResultStatus.FAILURE, err.getMessage()))
                );
    }
}
