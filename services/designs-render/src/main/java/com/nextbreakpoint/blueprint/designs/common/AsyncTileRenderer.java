package com.nextbreakpoint.blueprint.designs.common;

import com.nextbreakpoint.blueprint.common.events.avro.TileRenderRequested;
import io.vertx.rxjava.core.WorkerExecutor;
import lombok.extern.log4j.Log4j2;
import rx.Single;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static com.nextbreakpoint.blueprint.designs.common.Bucket.createBucketKey;

@Log4j2
public class AsyncTileRenderer {
    private final WorkerExecutor executor;
    private final TileRenderer renderer;

    public AsyncTileRenderer(WorkerExecutor executor, TileRenderer renderer) {
        this.executor = Objects.requireNonNull(executor);
        this.renderer = Objects.requireNonNull(renderer);
    }

    public Single<Result> renderImage(TileRenderRequested event) {
        return Single.from(execute(event));
    }

    private CompletableFuture<Result> execute(TileRenderRequested event) {
        return executor.executeBlocking(() -> render(event))
                .toCompletionStage().toCompletableFuture();
    }

    private Result render(TileRenderRequested event) {
        log.debug("Render image: {}", createBucketKey(event));
        final var result = renderer.renderImage(event);
        log.debug("Image rendered: {}", createBucketKey(event));
        return result;
    }
}
