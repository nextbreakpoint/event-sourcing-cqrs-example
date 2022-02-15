package com.nextbreakpoint.blueprint.designs.operations.delete;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.DesignDeleteRequested;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.common.vertx.KafkaEmitter;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import rx.Single;

import java.util.Objects;
import java.util.UUID;

public class DeleteDesignController implements Controller<DeleteDesignRequest, DeleteDesignResponse> {
    private static final Logger logger = LoggerFactory.getLogger(DeleteDesignController.class.getName());

    private final Mapper<DeleteDesignRequest, DesignDeleteRequested> inputMapper;
    private final MessageMapper<DesignDeleteRequested, OutputMessage> outputMapper;
    private final KafkaEmitter emitter;

    public DeleteDesignController(Mapper<DeleteDesignRequest, DesignDeleteRequested> inputMapper, MessageMapper<DesignDeleteRequested, OutputMessage> outputMapper, KafkaEmitter emitter) {
        this.emitter = Objects.requireNonNull(emitter);
        this.inputMapper = Objects.requireNonNull(inputMapper);
        this.outputMapper = Objects.requireNonNull(outputMapper);
    }

    @Override
    public Single<DeleteDesignResponse> onNext(DeleteDesignRequest request) {
        return Single.just(request)
                .map(this.inputMapper::transform)
                .doOnSuccess(event -> logger.info("Processing event " + event))
                .map(event -> outputMapper.transform(Tracing.of(null), event))
                .flatMap(emitter::onNext)
                .map(ignore -> new DeleteDesignResponse(request.getUuid(), ResultStatus.SUCCESS))
                .onErrorReturn(err -> new DeleteDesignResponse(request.getUuid(), ResultStatus.FAILURE, err.getMessage()));
    }
}
