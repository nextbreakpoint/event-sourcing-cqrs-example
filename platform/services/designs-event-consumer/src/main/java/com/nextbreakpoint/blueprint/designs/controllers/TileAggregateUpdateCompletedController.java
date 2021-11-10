package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import rx.Single;

public class TileAggregateUpdateCompletedController implements Controller<InputMessage, Void> {
    @Override
    public Single<Void> onNext(InputMessage message) {
        return Single.just(message).map(result -> null);
    }
}
